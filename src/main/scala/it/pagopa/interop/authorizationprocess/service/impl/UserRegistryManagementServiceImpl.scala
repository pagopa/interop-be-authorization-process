package it.pagopa.interop.authorizationprocess.service.impl

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.authorizationprocess.service.{UserRegistryManagementInvoker, UserRegistryManagementService}
import it.pagopa.interop.commons.logging.{ContextFieldsToLog, CanLogContextFields}
import it.pagopa.interop.selfcare.userregistry.client.api.UserApi
import it.pagopa.interop.selfcare.userregistry.client.invoker.{ApiKeyValue, ApiRequest}
import it.pagopa.interop.selfcare.userregistry.client.model.{Field, UserResource}

import java.util.UUID
import scala.concurrent.Future

final case class UserRegistryManagementServiceImpl(invoker: UserRegistryManagementInvoker, api: UserApi)(implicit
  apiKeyValue: ApiKeyValue
) extends UserRegistryManagementService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getUserById(id: UUID)(implicit contexts: Seq[(String, String)]): Future[UserResource] = {
    val request: ApiRequest[UserResource] =
      api.findByIdUsingGET(id, Seq(Field.name, Field.familyName, Field.fiscalCode))
    invoker.invoke(request, "Retrieve User by ID")
  }
}
