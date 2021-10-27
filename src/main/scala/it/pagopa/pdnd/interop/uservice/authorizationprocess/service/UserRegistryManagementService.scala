package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import it.pagopa.pdnd.interop.uservice.userregistrymanagement.client.model.{User, UserSeed}

import java.util.UUID
import scala.concurrent.Future

trait UserRegistryManagementService {

  def createUser(seed: UserSeed): Future[User]
  def getUserByExternalId(taxCode: String): Future[User]
  def getUserById(id: UUID): Future[User]
}
