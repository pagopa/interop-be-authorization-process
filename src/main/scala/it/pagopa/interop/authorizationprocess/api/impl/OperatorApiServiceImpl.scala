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
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.AkkaUtils.getFutureBearer
import it.pagopa.interop.commons.utils.TypeConversions.StringOps
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.{MissingBearer, ResourceNotFoundError}
import it.pagopa.interop.partymanagement.client.model.{Problem => _}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

final case class OperatorApiServiceImpl(
  authorizationManagementService: AuthorizationManagementService,
  partyManagementService: PartyManagementService
)(implicit ec: ExecutionContext)
    extends OperatorApiService {

  val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](LoggerFactory.getLogger(this.getClass))

  /** Code: 200, Message: returns the corresponding array of keys, DataType: ClientKeys
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client id not found, DataType: Problem
    */
  override def getClientOperatorKeys(clientId: String, operatorId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    logger.info("Getting client keys {} for operator {}", clientId, operatorId)
    val result = for {
      bearerToken   <- getFutureBearer(contexts)
      operatorUuid  <- operatorId.toFutureUUID
      relationships <- partyManagementService.getRelationshipsByPersonId(operatorUuid, Seq.empty)(bearerToken)
      clientUuid    <- clientId.toFutureUUID
      clientKeys    <- authorizationManagementService.getClientKeys(clientUuid)(bearerToken)
      operatorKeys = clientKeys.keys.filter(key => relationships.items.exists(_.id == key.relationshipId))
      keysResponse = authorizationmanagement.client.model.KeysResponse(operatorKeys)
    } yield ClientKeys(keysResponse.keys.map(AuthorizationManagementService.keyToApi))

    onComplete(result) {
      case Success(result)                                                   => getClientOperatorKeys200(result)
      case Failure(MissingBearer)                                            =>
        logger.error(
          s"Error while getting client ${clientId} keys for operator ${operatorId} keys - ${MissingBearer.getMessage}"
        )
        getClientOperatorKeys401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error(s"Error while getting client ${clientId} keys for operator ${operatorId} keys - ${ex.getMessage}")
        getClientOperatorKeys404(
          problemOf(StatusCodes.NotFound, ResourceNotFoundError(s"client id: $clientId, operator id: $operatorId"))
        )
      case Failure(NoResultsError)                                           =>
        logger.error(
          s"Error while getting client ${clientId} keys for operator ${operatorId} keys - ${NoResultsError.getMessage}"
        )
        getClientOperatorKeys404(problemOf(StatusCodes.NotFound, NoResultsError))
      case Failure(ex)                                                       =>
        logger.error(s"Error while getting client ${clientId} keys for operator ${operatorId} keys - ${ex.getMessage}")
        val error = problemOf(StatusCodes.InternalServerError, OperatorKeysRetrievalError)
        complete((error.status, error))
    }
  }

}
