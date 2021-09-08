package it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl

import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{
  AuthorizationManagementService,
  KeyManagementInvoker
}
import it.pagopa.pdnd.interop.uservice.keymanagement.client.api.{ClientApi, KeyApi}
import it.pagopa.pdnd.interop.uservice.keymanagement.client.invoker.ApiRequest
import it.pagopa.pdnd.interop.uservice.keymanagement.client.model.{Client, ClientSeed, OperatorSeed}
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class AuthorizationManagementServiceImpl(invoker: KeyManagementInvoker, clientApi: ClientApi, keyApi: KeyApi)(implicit
  ec: ExecutionContext
) extends AuthorizationManagementService {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /** Returns the expected audience defined by the producer of the corresponding agreementId.
    *
    * @param agreementId
    * @param description
    * @return
    */
  override def createClient(agreementId: UUID, description: String): Future[Client] = {
    val request: ApiRequest[Client] = clientApi.createClient(ClientSeed(agreementId, description))
    invoke(request, "Client creation")
  }

  override def getClient(clientId: String): Future[Client] = {
    val request: ApiRequest[Client] = clientApi.getClient(clientId)
    invoke(request, "Client retrieve")
  }

  override def listClients(
    offset: Option[Int],
    limit: Option[Int],
    agreementId: Option[UUID],
    operatorId: Option[UUID]
  ): Future[Seq[Client]] = {
    val request: ApiRequest[Seq[Client]] = clientApi.listClients(offset, limit, agreementId, operatorId)
    invoke(request, "Client list")
  }

  override def deleteClient(clientId: String): Future[Unit] = {
    val request: ApiRequest[Unit] = clientApi.deleteClient(clientId)
    invoke(request, "Client delete")
  }

  override def addOperator(clientId: UUID, operatorId: UUID): Future[Client] = {
    val request: ApiRequest[Client] = clientApi.addOperator(clientId, OperatorSeed(operatorId))
    invoke(request, "Operator addition to client")
  }

  override def removeClientOperator(clientId: UUID, operatorId: UUID): Future[Unit] = {
    val request: ApiRequest[Unit] = clientApi.removeClientOperator(clientId, operatorId)
    invoke(request, "Operator removal from client")
  }

  override def enableKey(clientId: UUID, kid: String): Future[Unit] = {
    val request: ApiRequest[Unit] = keyApi.enableKeyById(clientId, kid)
    invoke(request, "Key enable")
  }

  private def invoke[T](request: ApiRequest[T], logMessage: String): Future[T] =
    invoker
      .execute[T](request)
      .map { response =>
        logger.debug(s"$logMessage. Status code: ${response.code.toString}. Content: ${response.content.toString}")
        response.content
      }
      .recoverWith { case ex =>
        logger.error(s"$logMessage. Error: ${ex.getMessage}")
        Future.failed[T](ex)
      }
}
