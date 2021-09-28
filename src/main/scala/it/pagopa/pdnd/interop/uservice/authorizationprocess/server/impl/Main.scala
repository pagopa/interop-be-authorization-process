package it.pagopa.pdnd.interop.uservice.authorizationprocess.server.impl

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.management.scaladsl.AkkaManagement
import com.bettercloud.vault.Vault
import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.api.{AgreementApi => AgreementManagementApi}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.{AuthApi, ClientApi, OperatorApi}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.{
  AuthApiMarshallerImpl,
  AuthApiServiceImpl,
  ClientApiMarshallerImpl,
  ClientApiServiceImpl,
  OperatorApiMarshallerImpl,
  OperatorApiServiceImpl
}
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

trait JWTGenerator {
  lazy val vault: Vault                  = getVaultClient
  private val vaultService: VaultService = VaultServiceImpl(vault)
  val jwtGenerator: JWTGeneratorImpl     = JWTGeneratorImpl(vaultService)
}

trait JWTValidator { self: AuthorizationManagementAPI =>
  val jwtValidator: JWTValidatorImpl = JWTValidatorImpl(authorizationManagementService)
}

object Main
    extends App
    with CorsSupport
    with AgreementManagementAPI
    with AuthorizationManagementAPI
    with CatalogManagementAPI
    with PartyManagementAPI
    with JWTGenerator
    with JWTValidator
    with M2MAuthorizationService {

  Kamon.init()

  val authApi: AuthApi = new AuthApi(
    new AuthApiServiceImpl(
      jwtValidator,
      jwtGenerator,
      authorizationManagementService,
      agreementManagementService,
      catalogManagementService,
      m2mAuthorizationService
    ),
    new AuthApiMarshallerImpl(),
    SecurityDirectives.authenticateOAuth2("SecurityRealm", PassThroughAuthenticator)
  )

  val clientApi: ClientApi = new ClientApi(
    new ClientApiServiceImpl(
      authorizationManagementService,
      agreementManagementService,
      catalogManagementService,
      partyManagementService
    ),
    new ClientApiMarshallerImpl(),
    SecurityDirectives.authenticateOAuth2("SecurityRealm", Authenticator)
  )

  val operatorApi: OperatorApi = new OperatorApi(
    new OperatorApiServiceImpl(authorizationManagementService, partyManagementService),
    new OperatorApiMarshallerImpl(),
    SecurityDirectives.authenticateOAuth2("SecurityRealm", Authenticator)
  )

  locally {
    val _ = AkkaManagement.get(classicActorSystem).start()
  }

  val controller: Controller = new Controller(authApi, clientApi, operatorApi)

  val bindingFuture: Future[Http.ServerBinding] =
    Http().newServerAt("0.0.0.0", ApplicationConfiguration.serverPort).bind(corsHandler(controller.routes))

}
