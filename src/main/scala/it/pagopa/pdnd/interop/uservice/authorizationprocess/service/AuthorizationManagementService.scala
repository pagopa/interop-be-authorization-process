package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import it.pagopa.pdnd.interop.uservice.keymanagement.client.model.{Client, Key}

import java.util.UUID
import scala.concurrent.Future

trait AuthorizationManagementService {

  /** Returns the expected audience defined by the producer of the corresponding agreementId.
    *
    * @param agreementId
    * @return
    */

  def createClient(agreementId: UUID, description: String): Future[Client]
  def getClient(clientId: String): Future[Client]
  def listClients(
    offset: Option[Int],
    limit: Option[Int],
    agreementId: Option[UUID],
    operatorId: Option[UUID]
  ): Future[Seq[Client]]
  def deleteClient(clientId: String): Future[Unit]

  def addOperator(clientId: UUID, operatorId: UUID): Future[Client]
  def removeClientOperator(clientId: UUID, operatorId: UUID): Future[Unit]

  def getKey(clientId: UUID, kid: String): Future[Key]
  def deleteKey(clientId: UUID, kid: String): Future[Unit]
  def enableKey(clientId: UUID, kid: String): Future[Unit]
}
