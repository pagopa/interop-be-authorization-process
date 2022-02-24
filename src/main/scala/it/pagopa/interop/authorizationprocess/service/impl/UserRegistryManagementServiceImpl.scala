package it.pagopa.interop.authorizationprocess.service.impl

import it.pagopa.interop.authorizationprocess.service.{UserRegistryManagementInvoker, UserRegistryManagementService}
import it.pagopa.pdnd.interop.uservice.userregistrymanagement.client.api.UserApi
import it.pagopa.pdnd.interop.uservice.userregistrymanagement.client.invoker.{ApiKeyValue, ApiRequest}
import it.pagopa.pdnd.interop.uservice.userregistrymanagement.client.model.User
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.Future

final case class UserRegistryManagementServiceImpl(invoker: UserRegistryManagementInvoker, api: UserApi)(implicit
  apiKeyValue: ApiKeyValue
) extends UserRegistryManagementService {

  implicit val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def getUserById(id: UUID): Future[User] = {
    val request: ApiRequest[User] = api.getUserById(id)
    invoker.invoke(request, "Retrieve User by ID")
  }
}
