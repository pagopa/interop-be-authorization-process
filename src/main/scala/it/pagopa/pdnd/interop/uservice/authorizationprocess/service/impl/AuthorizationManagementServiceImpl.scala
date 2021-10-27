package it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl

import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{
  AuthorizationManagementService,
  KeyManagementInvoker
}
import it.pagopa.pdnd.interop.uservice.keymanagement.client.api.{ClientApi, KeyApi}
import it.pagopa.pdnd.interop.uservice.keymanagement.client.invoker.ApiRequest
import it.pagopa.pdnd.interop.uservice.keymanagement.client.model._
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class AuthorizationManagementServiceImpl(invoker: KeyManagementInvoker, clientApi: ClientApi, keyApi: KeyApi)(implicit
  ec: ExecutionContext
) extends AuthorizationManagementService {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createClient(
    eServiceId: UUID,
    consumerId: UUID,
    name: String,
    purposes: String,
    description: Option[String]
  ): Future[Client] = {
    val request: ApiRequest[Client] =
      clientApi.createClient(
        ClientSeed(
          eServiceId = eServiceId,
          consumerId = consumerId,
          name = name,
          purposes = purposes,
          description = description
        )
      )
    invoke(request, "Client creation")
  }

  override def getClient(clientId: UUID): Future[Client] = {
    val request: ApiRequest[Client] = clientApi.getClient(clientId.toString)
    invoke(request, "Client retrieve")
  }

  override def listClients(
    offset: Option[Int],
    limit: Option[Int],
    eServiceId: Option[UUID],
    relationshipId: Option[UUID],
    consumerId: Option[UUID]
  ): Future[Seq[Client]] = {
    val request: ApiRequest[Seq[Client]] = clientApi.listClients(offset, limit, eServiceId, relationshipId, consumerId)
    invoke(request, "Client list")
  }

  override def deleteClient(clientId: UUID): Future[Unit] = {
    val request: ApiRequest[Unit] = clientApi.deleteClient(clientId.toString)
    invoke(request, "Client delete")
  }

  override def activateClient(clientId: UUID): Future[Unit] = {
    val request: ApiRequest[Unit] = clientApi.activateClientById(clientId)
    invoke(request, "Client activation")
  }

  override def suspendClient(clientId: UUID): Future[Unit] = {
    val request: ApiRequest[Unit] = clientApi.suspendClientById(clientId)
    invoke(request, "Client suspension")
  }

  override def addRelationship(clientId: UUID, relationshipId: UUID): Future[Client] = {
    val request: ApiRequest[Client] = clientApi.addRelationship(clientId, PartyRelationshipSeed(relationshipId))
    invoke(request, "Operator addition to client")
  }

  override def removeClientRelationship(clientId: UUID, relationshipId: UUID): Future[Unit] = {
    val request: ApiRequest[Unit] = clientApi.removeClientRelationship(clientId, relationshipId)
    invoke(request, "Operator removal from client")
  }

  override def getKey(clientId: UUID, kid: String): Future[ClientKey] = {
    val request: ApiRequest[ClientKey] = keyApi.getClientKeyById(clientId, kid)
    invoke(request, "Key Retrieve")
  }

  override def deleteKey(clientId: UUID, kid: String): Future[Unit] = {
    val request: ApiRequest[Unit] = keyApi.deleteClientKeyById(clientId, kid)
    invoke(request, "Key Delete")
  }

  override def enableKey(clientId: UUID, kid: String): Future[Unit] = {
    val request: ApiRequest[Unit] = keyApi.enableKeyById(clientId, kid)
    invoke(request, "Key enable")
  }

  override def disableKey(clientId: UUID, kid: String): Future[Unit] = {
    val request: ApiRequest[Unit] = keyApi.disableKeyById(clientId, kid)
    invoke(request, "Key disable")
  }

  override def getClientKeys(clientId: UUID): Future[KeysResponse] = {
    val request: ApiRequest[KeysResponse] = keyApi.getClientKeys(clientId)
    invoke(request, "Client keys retrieve")
  }

  def getEncodedClientKey(clientId: UUID, kid: String): Future[EncodedClientKey] = {
    val request: ApiRequest[EncodedClientKey] = keyApi.getEncodedClientKeyById(clientId, kid)
    invoke(request, "Key Retrieve")
  }

  override def createKeys(clientId: UUID, keysSeeds: Seq[KeySeed]): Future[KeysResponse] = {
    val request: ApiRequest[KeysResponse] = keyApi.createKeys(clientId, keysSeeds)
    invoke(request, "Key creation")
  }

  private def invoke[T](request: ApiRequest[T], logMessage: String)(implicit m: Manifest[T]): Future[T] =
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
