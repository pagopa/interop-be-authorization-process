package it.pagopa.interop.authorizationprocess.service

import it.pagopa.interop.selfcare.userregistry.client.model.UserResource

import java.util.UUID
import scala.concurrent.Future

trait UserRegistryManagementService {

  def getUserById(id: UUID)(implicit contexts: Seq[(String, String)]): Future[UserResource]
}
