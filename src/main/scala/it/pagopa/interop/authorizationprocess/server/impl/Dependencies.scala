package it.pagopa.interop.authorizationprocess.server.impl

import it.pagopa.interop.authorizationmanagement.client.api.{
  ClientApi => AuthorizationClientApi,
  KeyApi => AuthorizationKeyApi,
  PurposeApi => AuthorizationPurposeApi
}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.ValidationRequestError
import it.pagopa.interop.commons.utils.OpenapiUtils
import it.pagopa.interop.authorizationprocess.api.impl.problemOf
import it.pagopa.interop.authorizationprocess.common.ApplicationConfiguration
import it.pagopa.interop.authorizationprocess.common.system.{classicActorSystem, executionContext}
import it.pagopa.interop.authorizationprocess.service._
import it.pagopa.interop.authorizationprocess.service.impl.{
  AgreementManagementServiceImpl,
  AuthorizationManagementServiceImpl,
  CatalogManagementServiceImpl,
  PartyManagementServiceImpl,
  PurposeManagementServiceImpl,
  UserRegistryManagementServiceImpl
}
import it.pagopa.interop.authorizationprocess.api.impl.{
  ClientApiMarshallerImpl,
  ClientApiServiceImpl,
  OperatorApiMarshallerImpl,
  OperatorApiServiceImpl
}
import it.pagopa.interop.authorizationprocess.api.{ClientApi, OperatorApi}
import it.pagopa.interop.partymanagement.client.api.{PartyApi => PartyManagementApi}
import it.pagopa.interop.agreementmanagement.client.api.{AgreementApi => AgreementManagementApi}
import it.pagopa.interop.catalogmanagement.client.api.{EServiceApi => CatalogManagementApi}
import it.pagopa.interop.purposemanagement.client.api.{PurposeApi => PurposeManagementApi}
import it.pagopa.pdnd.interop.uservice.userregistrymanagement.client.api.{UserApi => UserRegistryManagementApi}
import it.pagopa.pdnd.interop.uservice.userregistrymanagement.client.invoker.ApiKeyValue
import com.atlassian.oai.validator.report.ValidationReport
import akka.http.scaladsl.server.Route
import it.pagopa.interop.commons.jwt.service.JWTReader
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import scala.concurrent.Future
import it.pagopa.interop.commons.jwt.service.impl.{DefaultJWTReader, getClaimsVerifier}
import it.pagopa.interop.commons.jwt.{JWTConfiguration, KID, PublicKeysHolder, SerializedKey}
import it.pagopa.interop.commons.utils.TypeConversions.TryOps

trait Dependencies {
  val partyManagementService: PartyManagementService = PartyManagementServiceImpl(
    PartyManagementInvoker(),
    PartyManagementApi(ApplicationConfiguration.getPartyManagementURL)
  )

  implicit val apiKey: ApiKeyValue = ApiKeyValue(ApplicationConfiguration.userRegistryApiKey)
  val userRegistryManagementService: UserRegistryManagementServiceImpl = UserRegistryManagementServiceImpl(
    UserRegistryManagementInvoker(),
    UserRegistryManagementApi(ApplicationConfiguration.getUserRegistryManagementURL)
  )

  val authorizationManagementClientApi: AuthorizationClientApi       = AuthorizationClientApi(
    ApplicationConfiguration.getAuthorizationManagementURL
  )
  val authorizationManagementKeyApi: AuthorizationKeyApi             = AuthorizationKeyApi(
    ApplicationConfiguration.getAuthorizationManagementURL
  )
  val authorizationManagementService: AuthorizationManagementService = AuthorizationManagementServiceImpl(
    AuthorizationManagementInvoker(),
    AuthorizationClientApi(ApplicationConfiguration.getAuthorizationManagementURL),
    AuthorizationKeyApi(ApplicationConfiguration.getAuthorizationManagementURL),
    AuthorizationPurposeApi(ApplicationConfiguration.getAuthorizationManagementURL)
  )

  val agreementManagementService: AgreementManagementService = AgreementManagementServiceImpl(
    AgreementManagementInvoker(),
    AgreementManagementApi(ApplicationConfiguration.getAgreementManagementURL)
  )

  val catalogManagementService: CatalogManagementService = CatalogManagementServiceImpl(
    CatalogManagementInvoker(),
    CatalogManagementApi(ApplicationConfiguration.getCatalogManagementURL)
  )

  val purposeManagementService: PurposeManagementService = PurposeManagementServiceImpl(
    PurposeManagementInvoker(),
    PurposeManagementApi(ApplicationConfiguration.getPurposeManagementURL)
  )

  val validationExceptionToRoute: ValidationReport => Route = report => {
    val error =
      problemOf(StatusCodes.BadRequest, ValidationRequestError(OpenapiUtils.errorFromRequestValidationReport(report)))
    complete(error.status, error)(ClientApiMarshallerImpl.toEntityMarshallerProblem)
  }

  def clientApi(jwtReader: JWTReader): ClientApi = new ClientApi(
    ClientApiServiceImpl(
      authorizationManagementService,
      agreementManagementService,
      catalogManagementService,
      partyManagementService,
      purposeManagementService,
      userRegistryManagementService
    ),
    ClientApiMarshallerImpl,
    jwtReader.OAuth2JWTValidatorAsContexts
  )

  def operatorApi(jwtReader: JWTReader): OperatorApi = new OperatorApi(
    OperatorApiServiceImpl(authorizationManagementService, partyManagementService),
    OperatorApiMarshallerImpl,
    jwtReader.OAuth2JWTValidatorAsContexts
  )

  def getJwtValidator(): Future[JWTReader] = JWTConfiguration.jwtReader
    .loadKeyset()
    .toFuture
    .map(keyset =>
      new DefaultJWTReader with PublicKeysHolder {
        var publicKeyset: Map[KID, SerializedKey]                                        = keyset
        override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
          getClaimsVerifier(audience = ApplicationConfiguration.jwtAudience)
      }
    )

}
