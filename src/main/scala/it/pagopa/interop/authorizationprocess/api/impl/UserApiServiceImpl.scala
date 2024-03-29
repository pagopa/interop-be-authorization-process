package it.pagopa.interop.authorizationprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route
import cats.syntax.all._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.authorizationprocess.api.UserApiService
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationprocess.service._
import it.pagopa.interop.authorizationprocess.api.impl.UserApiHandlers._
import it.pagopa.interop.commons.jwt.{ADMIN_ROLE, SECURITY_ROLE, SUPPORT_ROLE, authorize}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.selfcare.v2.client.model.{Problem => _}
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.authorizationprocess.common.Adapters._
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.authorizationprocess.common.AuthorizationUtils._

import scala.concurrent.{ExecutionContext, Future}

final case class UserApiServiceImpl(
  authorizationManagementService: AuthorizationManagementService,
  selfcareV2Service: SelfcareV2Service
)(implicit ec: ExecutionContext, readModel: ReadModelService)
    extends UserApiService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getClientUserKeys(clientId: String, userId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerKeys: ToEntityMarshaller[Keys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, SUPPORT_ROLE) {
    val operationLabel: String = s"Getting client keys $clientId for user $userId"
    logger.info(operationLabel)

    val result: Future[Keys] = for {
      clientUuid <- clientId.toFutureUUID
      client     <- authorizationManagementService.getClient(clientUuid)
      _          <- assertIsClientConsumer(client).toFuture
      userUuid   <- userId.toFutureUUID
      clientKeys <- authorizationManagementService.getClientKeys(client.id)
      apiKeys = clientKeys.filter(userUuid.some == _.userId).map(_.toApi)
      keys <- apiKeys.traverse(_.toFuture)
    } yield Keys(keys = keys)

    onComplete(result) {
      getClientUserKeysResponse[Keys](operationLabel)(getClientUserKeys200)
    }
  }
}
