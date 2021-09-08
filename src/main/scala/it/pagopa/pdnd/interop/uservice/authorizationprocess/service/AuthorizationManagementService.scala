package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import it.pagopa.pdnd.interop.uservice.keymanagement.client.model.Client

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

  def addOperator(clientId: UUID, operatorId: UUID):
}
