package it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl

import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{
  UserRegistryManagementInvoker,
  UserRegistryManagementService
}
import it.pagopa.pdnd.interop.uservice.userregistrymanagement.client.api.UserApi
import it.pagopa.pdnd.interop.uservice.userregistrymanagement.client.invoker.ApiRequest
import it.pagopa.pdnd.interop.uservice.userregistrymanagement.client.model.{EmbeddedExternalId, User, UserId, UserSeed}
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

final case class UserRegistryManagementServiceImpl(invoker: UserRegistryManagementInvoker, api: UserApi)(implicit
  ec: ExecutionContext
) extends UserRegistryManagementService {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createUser(seed: UserSeed): Future[User] = {
    val request: ApiRequest[User] = api.createUser(seed)
    invoke(request, "Create User")
  }

  override def getUserIdByExternalId(taxCode: String): Future[UserId] = {
    val request: ApiRequest[UserId] = api.getUserIdByExternalId(EmbeddedExternalId(taxCode))
    invoke(request, "Retrieve UserID by External ID")
  }

  override def getUserById(id: UUID): Future[User] = {
    val request: ApiRequest[User] = api.getUserById(id)
    invoke(request, "Retrieve User by ID")
  }

  private def invoke[T](request: ApiRequest[T], logMessage: String)(implicit m: Manifest[T]): Future[T] =
    invoker
      .execute[T](request)
      .map { response =>
        logger.debug(s"$logMessage. Status code: ${response.code.toString}. Content: ${response.content.toString}")
        response.content
      }
      .recoverWith { case ex =>
        logger.error(s"$logMessage. Error: ${ex.getMessage}")
        Future.failed[T](ex)
      }
}
