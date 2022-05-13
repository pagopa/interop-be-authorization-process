package it.pagopa.interop.authorizationprocess.service

import it.pagopa.pdnd.interop.uservice.userregistrymanagement.client.model.User

import java.util.UUID
import scala.concurrent.Future

trait UserRegistryManagementService {

  def getUserById(id: UUID)(implicit contexts: Seq[(String, String)]): Future[User]
}
