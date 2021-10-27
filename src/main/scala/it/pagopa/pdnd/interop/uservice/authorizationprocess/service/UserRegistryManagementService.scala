package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import it.pagopa.pdnd.interop.uservice.userregistrymanagement.client.model.{User, UserId, UserSeed}

import java.util.UUID
import scala.concurrent.Future

trait UserRegistryManagementService {

  def createUser(seed: UserSeed): Future[User]
  def getUserIdByExternalId(taxCode: String): Future[UserId]
  def getUserById(id: UUID): Future[User]
}
