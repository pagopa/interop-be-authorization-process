package it.pagopa.pdnd.interop.uservice.authorizationprocess.server.impl

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.management.scaladsl.AkkaManagement
import com.bettercloud.vault.Vault
import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.api.{AgreementApi => AgreementManagementApi}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.AuthApi
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.{AuthApiMarshallerImpl, AuthApiServiceImpl}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.system.{
  Authenticator,
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
  VaultServiceImpl
}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.client.api.{EServiceApi => CatalogManagementApi}
import it.pagopa.pdnd.interop.uservice.keymanagement.client.api.{
  ClientApi => AuthorizationClientApi,
  KeyApi => AuthorizationKeyApi
}
import kamon.Kamon

import scala.concurrent.Future

trait AgreementManagementAPI {
  val agreementManagementService = new AgreementManagementServiceImpl(
    AgreementManagementInvoker(),
    AgreementManagementApi(ApplicationConfiguration.getAgreementProcessURL)
  )
}

trait CatalogManagementAPI {
  val catalogManagementService = new CatalogManagementServiceImpl(
    CatalogManagementInvoker(),
    CatalogManagementApi(ApplicationConfiguration.getCatalogProcessURL)
  )
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
    with CatalogManagementAPI
    with AuthorizationManagementAPI
    with JWTGenerator
    with JWTValidator {

  Kamon.init()

  val authApi: AuthApi = new AuthApi(
    new AuthApiServiceImpl(
      jwtValidator,
      jwtGenerator,
      agreementManagementService,
      catalogManagementService,
      authorizationManagementService
    ),
    new AuthApiMarshallerImpl(),
    SecurityDirectives.authenticateOAuth2("SecurityRealm", Authenticator)
  )

  locally {
    val _ = AkkaManagement.get(classicActorSystem).start()
  }

  val controller: Controller = new Controller(authApi)

  val bindingFuture: Future[Http.ServerBinding] =
    Http().newServerAt("0.0.0.0", ApplicationConfiguration.serverPort).bind(corsHandler(controller.routes))

}
