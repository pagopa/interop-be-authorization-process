package it.pagopa.interop.authorizationprocess.server.impl

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.SecurityDirectives
import com.atlassian.oai.validator.report.ValidationReport
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import it.pagopa.interop.agreementmanagement.client.api.{AgreementApi => AgreementManagementApi}
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
  OperatorApiServiceImpl,
  problemOf
}
import it.pagopa.interop.authorizationprocess.api.{ClientApi, HealthApi, OperatorApi}
import it.pagopa.interop.authorizationprocess.common.ApplicationConfiguration
import it.pagopa.interop.authorizationprocess.service._
import it.pagopa.interop.authorizationprocess.service.impl._
import it.pagopa.interop.catalogmanagement.client.api.{EServiceApi => CatalogManagementApi}
import it.pagopa.interop.commons.jwt.service.JWTReader
import it.pagopa.interop.commons.jwt.service.impl.{DefaultJWTReader, getClaimsVerifier}
import it.pagopa.interop.commons.jwt.{JWTConfiguration, KID, PublicKeysHolder, SerializedKey}
import it.pagopa.interop.commons.utils.TypeConversions.TryOps
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.ValidationRequestError
import it.pagopa.interop.commons.utils.{AkkaUtils, OpenapiUtils}
import it.pagopa.interop.purposemanagement.client.api.{PurposeApi => PurposeManagementApi}
import it.pagopa.interop.selfcare.partymanagement.client.api.{PartyApi => PartyManagementApi}
import it.pagopa.interop.selfcare.userregistry.client.api.{UserApi => UserRegistryManagementApi}
import it.pagopa.interop.selfcare.userregistry.client.invoker.ApiKeyValue

import scala.concurrent.{ExecutionContext, Future}

trait Dependencies {

  implicit val partyManagementApiKeyValue: PartyManagementApiKeyValue = PartyManagementApiKeyValue()

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

  def authorizationManagementService()(implicit
    actorSystem: ActorSystem[_],
    blockingEc: ExecutionContext
  ): AuthorizationManagementService = AuthorizationManagementServiceImpl(
    AuthorizationManagementInvoker()(actorSystem.classicSystem, blockingEc),
    AuthorizationClientApi(ApplicationConfiguration.getAuthorizationManagementURL),
    AuthorizationKeyApi(ApplicationConfiguration.getAuthorizationManagementURL),
    AuthorizationPurposeApi(ApplicationConfiguration.getAuthorizationManagementURL)
  )

  def agreementManagementService(implicit
    actorSystem: ActorSystem[_],
    ec: ExecutionContext
  ): AgreementManagementService =
    AgreementManagementServiceImpl(
      AgreementManagementInvoker()(actorSystem.classicSystem),
      AgreementManagementApi(ApplicationConfiguration.getAgreementManagementURL)
    )

  def catalogManagementService(implicit actorSystem: ActorSystem[_], ec: ExecutionContext): CatalogManagementService =
    CatalogManagementServiceImpl(
      CatalogManagementInvoker()(actorSystem.classicSystem),
      CatalogManagementApi(ApplicationConfiguration.getCatalogManagementURL)
    )

  def purposeManagementService(implicit actorSystem: ActorSystem[_], ec: ExecutionContext): PurposeManagementService =
    PurposeManagementServiceImpl(
      PurposeManagementInvoker()(actorSystem.classicSystem),
      PurposeManagementApi(ApplicationConfiguration.getPurposeManagementURL)
    )

  val validationExceptionToRoute: ValidationReport => Route = report => {
    val error =
      problemOf(StatusCodes.BadRequest, ValidationRequestError(OpenapiUtils.errorFromRequestValidationReport(report)))
    complete(error.status, error)(ClientApiMarshallerImpl.toEntityMarshallerProblem)
  }

  def clientApi(jwtReader: JWTReader)(implicit actorSystem: ActorSystem[_], ec: ExecutionContext): ClientApi =
    new ClientApi(
      ClientApiServiceImpl(
        authorizationManagementService(),
        agreementManagementService,
        catalogManagementService,
        partyManagementService(),
        purposeManagementService,
        userRegistryManagementService()
      ),
      ClientApiMarshallerImpl,
      jwtReader.OAuth2JWTValidatorAsContexts
    )

  def operatorApi(jwtReader: JWTReader)(implicit actorSystem: ActorSystem[_], ec: ExecutionContext): OperatorApi =
    new OperatorApi(
      OperatorApiServiceImpl(authorizationManagementService(), partyManagementService()),
      OperatorApiMarshallerImpl,
      jwtReader.OAuth2JWTValidatorAsContexts
    )

  def getJwtValidator()(implicit ec: ExecutionContext): Future[JWTReader] = JWTConfiguration.jwtReader
    .loadKeyset()
    .toFuture
    .map(keyset =>
      new DefaultJWTReader with PublicKeysHolder {
        var publicKeyset: Map[KID, SerializedKey]                                        = keyset
        override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
          getClaimsVerifier(audience = ApplicationConfiguration.jwtAudience)
      }
    )

  val healthApi: HealthApi = new HealthApi(
    new HealthServiceApiImpl(),
    HealthApiMarshallerImpl,
    SecurityDirectives.authenticateOAuth2("SecurityRealm", AkkaUtils.PassThroughAuthenticator),
    loggingEnabled = false
  )
}
