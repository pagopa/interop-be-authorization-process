package it.pagopa.interop.authorizationprocess.server.impl

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.SecurityDirectives
import com.atlassian.oai.validator.report.ValidationReport
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import it.pagopa.interop.authorizationmanagement.client.api.{
  ClientApi => AuthorizationClientApi,
  KeyApi => AuthorizationKeyApi,
  PurposeApi => AuthorizationPurposeApi
}
import it.pagopa.interop.authorizationprocess.api.impl.{
  ClientApiMarshallerImpl,
  ClientApiServiceImpl,
  HealthApiMarshallerImpl,
  HealthServiceApiImpl
}
import it.pagopa.interop.authorizationprocess.api.impl.UserApiServiceImpl
import it.pagopa.interop.authorizationprocess.api.impl.UserApiMarshallerImpl
import it.pagopa.interop.authorizationprocess.api.{ClientApi, HealthApi, UserApi}
import it.pagopa.interop.selfcare.v2.client.api.{InstitutionsApi, UsersApi}
import it.pagopa.interop.authorizationprocess.common.system.ApplicationConfiguration
import it.pagopa.interop.authorizationprocess.api.impl.serviceCode
import it.pagopa.interop.authorizationprocess.service._
import it.pagopa.interop.authorizationprocess.service.impl._
import it.pagopa.interop.commons.jwt.service.JWTReader
import it.pagopa.interop.commons.jwt.service.impl.{DefaultJWTReader, getClaimsVerifier}
import it.pagopa.interop.commons.jwt.{JWTConfiguration, KID, PublicKeysHolder, SerializedKey}
import it.pagopa.interop.commons.utils.TypeConversions.TryOps
import it.pagopa.interop.commons.utils.errors.{Problem => CommonProblem}
import it.pagopa.interop.commons.utils.{AkkaUtils, OpenapiUtils}
import it.pagopa.interop.commons.cqrs.service.{MongoDbReadModelService, ReadModelService}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier

trait Dependencies {

  implicit val loggerTI: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog]("OAuth2JWTValidatorAsContexts")

  implicit val readModelService: ReadModelService = new MongoDbReadModelService(
    ApplicationConfiguration.readModelConfig
  )
  val dateTimeSupplier: OffsetDateTimeSupplier    = OffsetDateTimeSupplier

  def selfcareV2ClientService()(implicit actorSystem: ActorSystem[_]): SelfcareV2ClientService =
    SelfcareV2ClientServiceImpl(
      SelfcareV2ClientInvoker()(actorSystem.classicSystem),
      InstitutionsApi(ApplicationConfiguration.getPartyManagementURL),
      UsersApi(ApplicationConfiguration.getPartyManagementURL)
    )(SelfcareV2ClientApiKeyValue())

  val authorizationManagementClientApi: AuthorizationClientApi = AuthorizationClientApi(
    ApplicationConfiguration.getAuthorizationManagementURL
  )

  val authorizationManagementKeyApi: AuthorizationKeyApi = AuthorizationKeyApi(
    ApplicationConfiguration.getAuthorizationManagementURL
  )

  def authorizationManagementService(
    blockingEc: ExecutionContextExecutor
  )(implicit actorSystem: ActorSystem[_]): AuthorizationManagementService = AuthorizationManagementServiceImpl(
    AuthorizationManagementInvoker(blockingEc)(actorSystem.classicSystem),
    AuthorizationClientApi(ApplicationConfiguration.getAuthorizationManagementURL),
    AuthorizationKeyApi(ApplicationConfiguration.getAuthorizationManagementURL),
    AuthorizationPurposeApi(ApplicationConfiguration.getAuthorizationManagementURL)
  )(blockingEc)

  val validationExceptionToRoute: ValidationReport => Route = report => {
    val error =
      CommonProblem(StatusCodes.BadRequest, OpenapiUtils.errorFromRequestValidationReport(report), serviceCode, None)
    complete(error.status, error)
  }

  def clientApi(jwtReader: JWTReader, blockingEc: ExecutionContextExecutor)(implicit
    actorSystem: ActorSystem[_],
    ec: ExecutionContext
  ): ClientApi = new ClientApi(
    ClientApiServiceImpl(
      authorizationManagementService(blockingEc),
      AgreementManagementServiceImpl,
      CatalogManagementServiceImpl,
      selfcareV2ClientService(),
      PurposeManagementServiceImpl,
      TenantManagementServiceImpl,
      dateTimeSupplier
    ),
    ClientApiMarshallerImpl,
    jwtReader.OAuth2JWTValidatorAsContexts
  )

  def userApi(jwtReader: JWTReader, blockingEc: ExecutionContextExecutor)(implicit
    actorSystem: ActorSystem[_],
    ec: ExecutionContext
  ): UserApi = new UserApi(
    UserApiServiceImpl(authorizationManagementService(blockingEc), selfcareV2ClientService()),
    UserApiMarshallerImpl,
    jwtReader.OAuth2JWTValidatorAsContexts(Logger.takingImplicit[ContextFieldsToLog]("OAuth2JWTValidatorAsContexts"))
  )

  def getJwtValidator(): Future[JWTReader] = JWTConfiguration.jwtReader
    .loadKeyset()
    .map(keyset =>
      new DefaultJWTReader with PublicKeysHolder {
        var publicKeyset: Map[KID, SerializedKey]                                        = keyset
        override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
          getClaimsVerifier(audience = ApplicationConfiguration.jwtAudience)
      }
    )
    .toFuture

  val healthApi: HealthApi = new HealthApi(
    new HealthServiceApiImpl(),
    HealthApiMarshallerImpl,
    SecurityDirectives.authenticateOAuth2("SecurityRealm", AkkaUtils.PassThroughAuthenticator),
    loggingEnabled = false
  )
}
