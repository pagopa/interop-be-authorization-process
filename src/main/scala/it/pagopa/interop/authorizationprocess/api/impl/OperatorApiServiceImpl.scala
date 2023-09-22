package it.pagopa.interop.authorizationprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.authorizationprocess.api.OperatorApiService
import OperatorApiHandlers._
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationprocess.service._
import it.pagopa.interop.commons.jwt.{ADMIN_ROLE, SECURITY_ROLE, SUPPORT_ROLE, authorize}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.selfcare.partymanagement.client.model.{Problem => _}
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.authorizationprocess.common.Adapters._
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.authorizationprocess.common.AuthorizationUtils._

import scala.concurrent.{ExecutionContext, Future}

final case class OperatorApiServiceImpl(
  authorizationManagementService: AuthorizationManagementService,
  partyManagementService: PartyManagementService
)(implicit ec: ExecutionContext, readModel: ReadModelService)
    extends OperatorApiService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getClientOperatorKeys(clientId: String, operatorId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerKeys: ToEntityMarshaller[Keys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, SUPPORT_ROLE) {
    val operationLabel: String = s"Getting client keys $clientId for operator $operatorId"
    logger.info(operationLabel)

    val result: Future[Keys] = for {
      clientUuid    <- clientId.toFutureUUID
      client        <- authorizationManagementService.getClient(clientUuid)
      _             <- assertIsClientConsumer(client).toFuture
      operatorUuid  <- operatorId.toFutureUUID
      relationships <- partyManagementService.getRelationshipsByPersonId(operatorUuid, Seq.empty)
      clientUuid    <- clientId.toFutureUUID
      clientKeys    <- authorizationManagementService.getClientKeys(clientUuid)
      keys = clientKeys
        .filter(key => relationships.items.exists(_.id == key.relationshipId))

    } yield Keys(keys.map(_.toApi))

    onComplete(result) {
      getClientOperatorKeysResponse[Keys](operationLabel)(getClientOperatorKeys200)
    }
  }
}
