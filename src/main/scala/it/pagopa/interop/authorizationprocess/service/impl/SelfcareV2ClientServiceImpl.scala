package it.pagopa.interop.authorizationprocess.service.impl

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.authorizationprocess.service.{
  SelfcareV2ClientInvoker,
  SelfcareV2ClientApiKeyValue,
  SelfcareV2ClientService
}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.selfcare.v2.client.api.{InstitutionsApi, UsersApi}
import it.pagopa.interop.selfcare.v2.client.model.UserResource
import it.pagopa.interop.selfcare.v2.client.invoker.{ApiRequest, ApiError}
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors.InstitutionNotFound

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

  override def getInstitutionProductUsers(
    selfcareId: UUID,
    requesterId: UUID,
    userId: Option[UUID],
    roles: Seq[String]
  )(implicit contexts: Seq[(String, String)], ec: ExecutionContext): Future[Seq[UserResource]] = {
    val request: ApiRequest[Seq[UserResource]] =
      institutionsApi.getInstitutionProductUsersUsingGET(
        institutionId = selfcareId.toString,
        userIdForAuth = requesterId.toString,
        userId = userId.map(_.toString),
        productRoles = roles
      )
    invoker
      .invoke(
        request,
        s"Retrieving User with istitution id $selfcareId, requesterId $requesterId, user $userId, for roles $roles"
      )
      .recoverWith {
        case err: ApiError[_] if err.code == 404 => Future.failed(InstitutionNotFound(selfcareId))
      }
  }
}
