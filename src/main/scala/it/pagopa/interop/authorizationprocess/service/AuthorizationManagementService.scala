package it.pagopa.interop.authorizationprocess.service

import it.pagopa.interop.authorizationmanagement.client.model._
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.authorizationmanagement.model.client.{PersistentClientKind, PersistentClient}
import it.pagopa.interop.authorizationmanagement.model.key.PersistentKey
import it.pagopa.interop.authorizationprocess.common.readmodel.PaginatedResult
import it.pagopa.interop.authorizationprocess.common.readmodel.model.ReadModelClientWithKeys

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}

trait AuthorizationManagementService {

  def createClient(
    consumerId: UUID,
    name: String,
    description: Option[String],
    kind: ClientKind,
    createdAt: OffsetDateTime,
    members: Seq[UUID]
  )(implicit contexts: Seq[(String, String)]): Future[ManagementClient]

  def getClient(clientId: UUID)(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PersistentClient]

  def deleteClient(clientId: UUID)(implicit contexts: Seq[(String, String)]): Future[Unit]

  def addRelationship(clientId: UUID, relationshipId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[ManagementClient]

  def removeClientRelationship(clientId: UUID, relationshipId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[Unit]

  def getClientKey(clientId: UUID, kid: String)(implicit
    ec: ExecutionContext,
    readModel: ReadModelService
  ): Future[PersistentKey]

  def getClientKeys(
    clientId: UUID
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[Seq[PersistentKey]]

  def createKeys(clientId: UUID, keysSeeds: Seq[KeySeed])(implicit contexts: Seq[(String, String)]): Future[Keys]

  def deleteKey(clientId: UUID, kid: String)(implicit contexts: Seq[(String, String)]): Future[Unit]

  def addClientPurpose(clientId: UUID, purposeSeed: PurposeSeed)(implicit
    contexts: Seq[(String, String)]
  ): Future[Purpose]

  def removeClientPurpose(clientId: UUID, purposeId: UUID)(implicit contexts: Seq[(String, String)]): Future[Unit]

  def getClientsByPurpose(
    purposeId: UUID
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[Seq[PersistentClient]]

  def getClientsWithKeys(
    name: Option[String],
    relationshipIds: List[UUID],
    consumerId: UUID,
    purposeId: Option[UUID],
    kind: Option[PersistentClientKind],
    offset: Int,
    limit: Int
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PaginatedResult[ReadModelClientWithKeys]]

  def getClients(
    name: Option[String],
    relationshipIds: List[UUID],
    consumerId: UUID,
    purposeId: Option[UUID],
    kind: Option[PersistentClientKind],
    offset: Int,
    limit: Int
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PaginatedResult[PersistentClient]]
}
