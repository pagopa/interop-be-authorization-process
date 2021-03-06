package it.pagopa.interop.authorizationprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.authorizationmanagement
import it.pagopa.interop.authorizationmanagement.client.invoker.{ApiError => AuthorizationManagementApiError}
import it.pagopa.interop.authorizationprocess.api.OperatorApiService
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors._
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationprocess.service._
import it.pagopa.interop.commons.jwt.{ADMIN_ROLE, SECURITY_ROLE}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.TypeConversions.StringOps
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.{
  MissingBearer,
  MissingUserId,
  ResourceNotFoundError
}
import it.pagopa.interop.selfcare.partymanagement.client.model.{Problem => _}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

final case class OperatorApiServiceImpl(
  authorizationManagementService: AuthorizationManagementService,
  partyManagementService: PartyManagementService
)(implicit ec: ExecutionContext)
    extends OperatorApiService {

  val logger: LoggerTakingImplicit[ContextFieldsToLog] = Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  /** Code: 200, Message: returns the corresponding array of keys, DataType: ClientKeys
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client id not found, DataType: Problem
    */
  override def getClientOperatorKeys(clientId: String, operatorId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE) {
    logger.info("Getting client keys {} for operator {}", clientId, operatorId)
    val result = for {
      operatorUuid  <- operatorId.toFutureUUID
      relationships <- partyManagementService.getRelationshipsByPersonId(operatorUuid, Seq.empty)
      clientUuid    <- clientId.toFutureUUID
      clientKeys    <- authorizationManagementService.getClientKeys(clientUuid)(contexts)
      operatorKeys = clientKeys.keys.filter(key => relationships.items.exists(_.id == key.relationshipId))
      keysResponse = authorizationmanagement.client.model.KeysResponse(operatorKeys)
    } yield ClientKeys(keysResponse.keys.map(AuthorizationManagementService.keyToApi))

    onComplete(result) {
      case Success(result)                                                   => getClientOperatorKeys200(result)
      case Failure(MissingUserId)                                            =>
        logger.error(s"Error while getting client $clientId keys for operator $operatorId keys", MissingUserId)
        getClientOperatorKeys401(problemOf(StatusCodes.Unauthorized, MissingUserId))
      case Failure(MissingBearer)                                            =>
        logger.error(s"Error while getting client $clientId keys for operator $operatorId keys", MissingBearer)
        getClientOperatorKeys401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error(s"Error while getting client $clientId keys for operator $operatorId keys", ex)
        getClientOperatorKeys404(
          problemOf(StatusCodes.NotFound, ResourceNotFoundError(s"client id: $clientId, operator id: $operatorId"))
        )
      case Failure(NoResultsError)                                           =>
        logger.error(s"Error while getting client $clientId keys for operator $operatorId keys", NoResultsError)
        getClientOperatorKeys404(problemOf(StatusCodes.NotFound, NoResultsError))
      case Failure(ex)                                                       =>
        logger.error(s"Error while getting client $clientId keys for operator $operatorId keys", ex)
        val error = problemOf(StatusCodes.InternalServerError, OperatorKeysRetrievalError)
        complete((error.status, error))
    }
  }

}
