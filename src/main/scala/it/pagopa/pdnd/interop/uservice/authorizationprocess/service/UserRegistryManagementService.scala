package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import it.pagopa.pdnd.interop.uservice.userregistrymanagement.client.model.User

import java.util.UUID
import scala.concurrent.Future

trait UserRegistryManagementService {

  def getUserById(id: UUID): Future[User]
}
