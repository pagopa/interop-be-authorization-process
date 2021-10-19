package it.pagopa.pdnd.interop.uservice.authorizationprocess.server.impl

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.management.scaladsl.AkkaManagement
import com.bettercloud.vault.Vault
import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.api.{AgreementApi => AgreementManagementApi}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.{
  AuthApiMarshallerImpl,
  AuthApiServiceImpl,
  ClientApiMarshallerImpl,
  ClientApiServiceImpl,
  OperatorApiMarshallerImpl,
  OperatorApiServiceImpl,
  WellKnownApiMarshallerImpl,
  WellKnownApiServiceImpl
}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.{AuthApi, ClientApi, OperatorApi, WellKnownApi}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.system.{
  Authenticator,
  PassThroughAuthenticator,
  classicActorSystem,
  executionContext
}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.{ApplicationConfiguration, CorsSupport}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.server.Controller
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl.{
  AgreementManagementServiceImpl,
  AuthorizationManagementServiceImpl,
  CatalogManagementServiceImpl,
  JWTGeneratorImpl,
  JWTValidatorImpl,
  M2MAuthorizationServiceImpl,
  PartyManagementServiceImpl,
  VaultServiceImpl
}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.client.api.{EServiceApi => CatalogManagementApi}
import it.pagopa.pdnd.interop.uservice.keymanagement.client.api.{
  ClientApi => AuthorizationClientApi,
  KeyApi => AuthorizationKeyApi
}
import it.pagopa.pdnd.interop.uservice.partymanagement.client.api.{PartyApi => PartyManagementApi}
import kamon.Kamon

import scala.concurrent.Future

trait AgreementManagementAPI {
  val agreementManagementService = new AgreementManagementServiceImpl(
    AgreementManagementInvoker(),
    AgreementManagementApi(ApplicationConfiguration.getAgreementManagementURL)
  )
}

trait CatalogManagementAPI {
  val catalogManagementService = new CatalogManagementServiceImpl(
    CatalogManagementInvoker(),
    CatalogManagementApi(ApplicationConfiguration.getCatalogManagementURL)
  )
}

trait PartyManagementAPI {
  val partyManagementService = new PartyManagementServiceImpl(
    PartyManagementInvoker(),
    PartyManagementApi(ApplicationConfiguration.getPartyManagementURL)
  )
}

trait M2MAuthorizationService {
  val m2mAuthorizationService: M2MAuthorizationServiceImpl = M2MAuthorizationServiceImpl()
}

trait AuthorizationManagementAPI {
  val authorizationManagementClientApi: AuthorizationClientApi = AuthorizationClientApi(
    ApplicationConfiguration.getAuthorizationManagementURL
  )
  val authorizationManagementKeyApi: AuthorizationKeyApi = AuthorizationKeyApi(
    ApplicationConfiguration.getAuthorizationManagementURL
  )
  val authorizationManagementService = new AuthorizationManagementServiceImpl(
    AuthorizationManagementInvoker(),
    AuthorizationClientApi(ApplicationConfiguration.getAuthorizationManagementURL),
    AuthorizationKeyApi(ApplicationConfiguration.getAuthorizationManagementURL)
  )
}

trait VaultServiceDependency {
  lazy val vault: Vault              = getVaultClient
  val vaultService: VaultServiceImpl = VaultServiceImpl(vault)
}

trait JWTGeneratorDependency { self: VaultServiceDependency =>
  val jwtGenerator: JWTGeneratorImpl = JWTGeneratorImpl(vaultService)
}

trait JWTValidatorDependency { self: AuthorizationManagementAPI =>
  val jwtValidator: JWTValidatorImpl = JWTValidatorImpl(authorizationManagementService)
}

object Main
    extends App
    with CorsSupport
    with VaultServiceDependency
    with AgreementManagementAPI
    with AuthorizationManagementAPI
    with CatalogManagementAPI
    with PartyManagementAPI
    with JWTGeneratorDependency
    with JWTValidatorDependency
    with M2MAuthorizationService {

  Kamon.init()

  val authApi: AuthApi = new AuthApi(
    AuthApiServiceImpl(
      jwtValidator,
      jwtGenerator,
      authorizationManagementService,
      agreementManagementService,
      catalogManagementService,
      m2mAuthorizationService
    ),
    AuthApiMarshallerImpl,
    SecurityDirectives.authenticateOAuth2("SecurityRealm", PassThroughAuthenticator)
  )

  val clientApi: ClientApi = new ClientApi(
    ClientApiServiceImpl(
      authorizationManagementService,
      agreementManagementService,
      catalogManagementService,
      partyManagementService
    ),
    ClientApiMarshallerImpl,
    SecurityDirectives.authenticateOAuth2("SecurityRealm", Authenticator)
  )

  val operatorApi: OperatorApi = new OperatorApi(
    OperatorApiServiceImpl(authorizationManagementService, partyManagementService),
    OperatorApiMarshallerImpl,
    SecurityDirectives.authenticateOAuth2("SecurityRealm", Authenticator)
  )

  val wellKnownApi: WellKnownApi = new WellKnownApi(
    WellKnownApiServiceImpl(vaultService = vaultService),
    WellKnownApiMarshallerImpl,
    SecurityDirectives.authenticateOAuth2("SecurityRealm", PassThroughAuthenticator)
  )

  locally {
    val _ = AkkaManagement.get(classicActorSystem).start()
  }

  val controller: Controller = new Controller(authApi, clientApi, operatorApi, wellKnownApi)

  val bindingFuture: Future[Http.ServerBinding] =
    Http().newServerAt("0.0.0.0", ApplicationConfiguration.serverPort).bind(corsHandler(controller.routes))

}
