package it.pagopa.interop.authorizationprocess.service.impl

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.authorizationprocess.service.{
  SelfcareV2ClientInvoker,
  SelfcareV2ClientApiKeyValue,
  SelfcareV2ClientService
}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.selfcare.v2.client.api.{InstitutionsApi, UsersApi}
import it.pagopa.interop.selfcare.v2.client.model.{UserResource, UserResponse}
import it.pagopa.interop.selfcare.v2.client.invoker.{ApiRequest, ApiError}
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors.{UserNotFound, InstitutionNotFound}
import cats.syntax.all._

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}

final case class SelfcareV2ClientServiceImpl(
  invoker: SelfcareV2ClientInvoker,
  institutionsApi: InstitutionsApi,
  usersApi: UsersApi
)(implicit val selfcareV2ClientApiKeyValue: SelfcareV2ClientApiKeyValue)
    extends SelfcareV2ClientService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getInstitutionProductUsers(selfcareId: UUID, personId: UUID, userId: UUID, productRoles: Seq[String])(
    implicit
    contexts: Seq[(String, String)],
    ec: ExecutionContext
  ): Future[Seq[UserResource]] = {
    val request: ApiRequest[Seq[UserResource]] =
      institutionsApi.getInstitutionProductUsersUsingGET(
        institutionId = selfcareId.toString,
        userIdForAuth = personId.toString,
        userId = userId.toString.some,
        productRoles = productRoles
      )
    invoker
      .invoke(
        request,
        s"Retrieving User with istitution id $selfcareId, personId $personId, user $userId, for roles $productRoles"
      )
      .recoverWith {
        case err: ApiError[_] if err.code == 404 => Future.failed(InstitutionNotFound(selfcareId))
      }
  }

  override def getUserById(selfcareId: UUID, userId: UUID)(implicit
    contexts: Seq[(String, String)],
    ec: ExecutionContext
  ): Future[UserResponse] = {
    val request: ApiRequest[UserResponse] =
      usersApi.getUserInfoUsingGET(id = userId.toString, institutionId = selfcareId.toString.some)
    invoker
      .invoke(request, s"Retrieving User with with istitution id $selfcareId, user $userId")
      .recoverWith {
        case err: ApiError[_] if err.code == 404 => Future.failed(UserNotFound(selfcareId, userId))
      }
  }
}
