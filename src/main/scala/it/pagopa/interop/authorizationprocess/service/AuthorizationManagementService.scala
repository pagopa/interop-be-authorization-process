package it.pagopa.interop.authorizationprocess.service

import it.pagopa.interop.authorizationmanagement.client.model._

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.Future

trait AuthorizationManagementService {

  def createClient(
    consumerId: UUID,
    name: String,
    description: Option[String],
    kind: ClientKind,
    createdAt: OffsetDateTime
  )(implicit contexts: Seq[(String, String)]): Future[ManagementClient]

  def getClient(clientId: UUID)(implicit contexts: Seq[(String, String)]): Future[ManagementClient]

  def deleteClient(clientId: UUID)(implicit contexts: Seq[(String, String)]): Future[Unit]

  def addRelationship(clientId: UUID, relationshipId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[ManagementClient]

  def removeClientRelationship(clientId: UUID, relationshipId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[Unit]

  def getKey(clientId: UUID, kid: String)(implicit contexts: Seq[(String, String)]): Future[ClientKey]

  def getClientKeys(clientId: UUID)(implicit contexts: Seq[(String, String)]): Future[KeysResponse]

  def createKeys(clientId: UUID, keysSeeds: Seq[KeySeed])(implicit
    contexts: Seq[(String, String)]
  ): Future[KeysResponse]

  def deleteKey(clientId: UUID, kid: String)(implicit contexts: Seq[(String, String)]): Future[Unit]

  def getEncodedClientKey(clientId: UUID, kid: String)(implicit
    contexts: Seq[(String, String)]
  ): Future[EncodedClientKey]

  def addClientPurpose(clientId: UUID, purposeSeed: PurposeSeed)(implicit
    contexts: Seq[(String, String)]
  ): Future[Purpose]

  def removeClientPurpose(clientId: UUID, purposeId: UUID)(implicit contexts: Seq[(String, String)]): Future[Unit]
}
