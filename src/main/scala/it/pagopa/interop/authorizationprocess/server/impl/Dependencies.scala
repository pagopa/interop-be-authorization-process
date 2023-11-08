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
  HealthServiceApiImpl,
  OperatorApiMarshallerImpl,
  OperatorApiServiceImpl
}
import it.pagopa.interop.authorizationprocess.api.{ClientApi, HealthApi, OperatorApi}
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
import it.pagopa.interop.selfcare.partymanagement.client.api.{PartyApi => PartyManagementApi}
import it.pagopa.interop.selfcare.userregistry.client.api.{UserApi => UserRegistryManagementApi}
import it.pagopa.interop.selfcare.userregistry.client.invoker.ApiKeyValue
import it.pagopa.interop.commons.cqrs.service.{MongoDbReadModelService, ReadModelService}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier

trait Dependencies {

  implicit val loggerTI: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog]("OAuth2JWTValidatorAsContexts")

  implicit val partyManagementApiKeyValue: PartyManagementApiKeyValue = PartyManagementApiKeyValue()
  implicit val readModelService: ReadModelService                     = new MongoDbReadModelService(
    ApplicationConfiguration.readModelConfig
  )
  val dateTimeSupplier: OffsetDateTimeSupplier                        = OffsetDateTimeSupplier

  def partyManagementService()(implicit actorSystem: ActorSystem[_]): PartyManagementService =
    PartyManagementServiceImpl(
      PartyManagementInvoker()(actorSystem.classicSystem),
      PartyManagementApi(ApplicationConfiguration.getPartyManagementURL)
    )

  def userRegistryManagementService()(implicit actorSystem: ActorSystem[_]): UserRegistryManagementServiceImpl =
    UserRegistryManagementServiceImpl(
      UserRegistryManagementInvoker()(actorSystem.classicSystem),
      UserRegistryManagementApi(ApplicationConfiguration.getUserRegistryManagementURL)
    )(ApiKeyValue(ApplicationConfiguration.userRegistryApiKey))

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
      partyManagementService(),
      PurposeManagementServiceImpl,
      userRegistryManagementService(),
      TenantManagementServiceImpl,
      dateTimeSupplier
    ),
    ClientApiMarshallerImpl,
    jwtReader.OAuth2JWTValidatorAsContexts
  )

  def operatorApi(jwtReader: JWTReader, blockingEc: ExecutionContextExecutor)(implicit
    actorSystem: ActorSystem[_],
    ec: ExecutionContext
  ): OperatorApi = new OperatorApi(
    OperatorApiServiceImpl(authorizationManagementService(blockingEc), partyManagementService()),
    OperatorApiMarshallerImpl,
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
