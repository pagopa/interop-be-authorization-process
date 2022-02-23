package it.pagopa.interop.authorizationprocess.service.impl

import it.pagopa.interop.authorizationmanagement.client.api.{ClientApi, KeyApi, PurposeApi}
import it.pagopa.interop.authorizationmanagement.client.invoker.{ApiRequest, BearerToken}
import it.pagopa.interop.authorizationmanagement.client.model._
import it.pagopa.interop.authorizationprocess.service.{AuthorizationManagementInvoker, AuthorizationManagementService}
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.Future

final case class AuthorizationManagementServiceImpl(
  invoker: AuthorizationManagementInvoker,
  clientApi: ClientApi,
  keyApi: KeyApi,
  purposeApi: PurposeApi
) extends AuthorizationManagementService {

  implicit val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createClient(consumerId: UUID, name: String, description: Option[String], kind: ClientKind)(
    bearer: String
  ): Future[Client] = {
    val request: ApiRequest[Client] =
      clientApi.createClient(ClientSeed(consumerId = consumerId, name = name, description = description, kind = kind))(
        BearerToken(bearer)
      )
    invoker.invoke(request, "Client creation")
  }

  override def getClient(clientId: UUID)(bearer: String): Future[Client] = {
    val request: ApiRequest[Client] = clientApi.getClient(clientId)(BearerToken(bearer))
    invoker.invoke(request, "Client retrieve")
  }

  override def listClients(
    offset: Option[Int],
    limit: Option[Int],
    relationshipId: Option[UUID],
    consumerId: Option[UUID],
    kind: Option[ClientKind] = None
  )(bearer: String): Future[Seq[Client]] = {
    val request: ApiRequest[Seq[Client]] =
      clientApi.listClients(offset, limit, relationshipId, consumerId, kind)(BearerToken(bearer))
    invoker.invoke(request, "Client list")
  }

  override def deleteClient(clientId: UUID)(bearer: String): Future[Unit] = {
    val request: ApiRequest[Unit] = clientApi.deleteClient(clientId.toString)(BearerToken(bearer))
    invoker.invoke(request, "Client delete")
  }

  override def addRelationship(clientId: UUID, relationshipId: UUID)(bearer: String): Future[Client] = {
    val request: ApiRequest[Client] =
      clientApi.addRelationship(clientId, PartyRelationshipSeed(relationshipId))(BearerToken(bearer))
    invoker.invoke(request, "Operator addition to client")
  }

  override def removeClientRelationship(clientId: UUID, relationshipId: UUID)(bearer: String): Future[Unit] = {
    val request: ApiRequest[Unit] = clientApi.removeClientRelationship(clientId, relationshipId)(BearerToken(bearer))
    invoker.invoke(request, "Operator removal from client")
  }

  override def getKey(clientId: UUID, kid: String)(bearer: String): Future[ClientKey] = {
    val request: ApiRequest[ClientKey] = keyApi.getClientKeyById(clientId, kid)(BearerToken(bearer))
    invoker.invoke(request, "Key Retrieve")
  }

  override def deleteKey(clientId: UUID, kid: String)(bearer: String): Future[Unit] = {
    val request: ApiRequest[Unit] = keyApi.deleteClientKeyById(clientId, kid)(BearerToken(bearer))
    invoker.invoke(request, "Key Delete")
  }

  override def getClientKeys(clientId: UUID)(bearer: String): Future[KeysResponse] = {
    val request: ApiRequest[KeysResponse] = keyApi.getClientKeys(clientId)(BearerToken(bearer))
    invoker.invoke(request, "Client keys retrieve")
  }

  def getEncodedClientKey(clientId: UUID, kid: String)(bearer: String): Future[EncodedClientKey] = {
    val request: ApiRequest[EncodedClientKey] = keyApi.getEncodedClientKeyById(clientId, kid)(BearerToken(bearer))
    invoker.invoke(request, "Key Retrieve")
  }

  override def createKeys(clientId: UUID, keysSeeds: Seq[KeySeed])(bearer: String): Future[KeysResponse] = {
    val request: ApiRequest[KeysResponse] = keyApi.createKeys(clientId, keysSeeds)(BearerToken(bearer))
    invoker.invoke(request, "Key creation")
  }

  override def addClientPurpose(clientId: UUID, purposeSeed: PurposeSeed)(bearer: String): Future[Purpose] = {
    val request: ApiRequest[Purpose] =
      purposeApi.addClientPurpose(clientId, purposeSeed)(BearerToken(bearer))
    invoker.invoke(request, "Purpose addition to client")
  }

  override def removeClientPurpose(clientId: UUID, purposeId: UUID)(bearer: String): Future[Unit] = {
    val request: ApiRequest[Unit] =
      purposeApi.removeClientPurpose(clientId, purposeId)(BearerToken(bearer))
    invoker.invoke(request, "Purpose remove from client")
  }
}
