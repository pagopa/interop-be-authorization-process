package it.pagopa.pdnd.interop.uservice.authorizationprocess.server.impl

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.management.scaladsl.AkkaManagement
import com.bettercloud.vault.Vault
import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.api.{AgreementApi => AgreementManagementApi}
import it.pagopa.pdnd.interop.uservice.agreementprocess.client.api.{ProcessApi => AgreementProcessApi}
import it.pagopa.pdnd.interop.uservice.keymanagement.client.api.{ClientApi => AuthorizationClientApi}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.AuthApi
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.{AuthApiMarshallerImpl, AuthApiServiceImpl}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.system.{
  Authenticator,
  classicActorSystem,
  executionContext
}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.{ApplicationConfiguration, CorsSupport}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.server.Controller
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl.{
  AgreementManagementServiceImpl,
  AgreementProcessServiceImpl,
  AuthorizationManagementServiceImpl,
  JWTGeneratorImpl,
  JWTValidatorImpl,
  KeyManagerImpl,
  VaultServiceImpl
}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service._
import it.pagopa.pdnd.interop.uservice.keymanagement.client.api.KeyApi
import kamon.Kamon

import scala.concurrent.Future

trait AgreementProcessAPI {
  val agreementProcessService = new AgreementProcessServiceImpl(
    AgreementProcessInvoker(),
    AgreementProcessApi(ApplicationConfiguration.getAgreementProcessURL)
  )
}

trait AgreementManagementAPI {
  val agreementManagementService = new AgreementManagementServiceImpl(
    AgreementManagementInvoker(),
    AgreementManagementApi(ApplicationConfiguration.getAgreementManagementURL)
  )
}

trait AuthorizationManagementAPI {
  val authorizationManagementService = new AuthorizationManagementServiceImpl(
    AuthorizationManagementInvoker(),
    AuthorizationClientApi(ApplicationConfiguration.getAuthorizationManagementURL)
  )
}

trait JWTGenerator {
  lazy val vault: Vault                  = getVaultClient
  private val vaultService: VaultService = VaultServiceImpl(vault)
  val jwtGenerator: JWTGeneratorImpl     = JWTGeneratorImpl(vaultService)
}

trait JWTValidator {
  private val invoker: KeyManagementInvoker = KeyManagementInvoker()
  private val keyApi: KeyApi                = KeyApi(ApplicationConfiguration.getKeyManagementUrl)
  private val keyManager: KeyManager        = KeyManagerImpl(invoker, keyApi)
  val jwtValidator: JWTValidatorImpl        = JWTValidatorImpl(keyManager)
}

object Main
    extends App
    with CorsSupport
    with AgreementProcessAPI
    with AgreementManagementAPI
    with AuthorizationManagementAPI
    with JWTGenerator
    with JWTValidator {

  Kamon.init()

  val authApi: AuthApi = new AuthApi(
    new AuthApiServiceImpl(
      jwtValidator,
      jwtGenerator,
      agreementProcessService,
      agreementManagementService,
      authorizationManagementService
    ),
    new AuthApiMarshallerImpl(),
    SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
  )

  locally {
    val _ = AkkaManagement.get(classicActorSystem).start()
  }

  val controller: Controller = new Controller(authApi)

  val bindingFuture: Future[Http.ServerBinding] =
    Http().newServerAt("0.0.0.0", ApplicationConfiguration.serverPort).bind(corsHandler(controller.routes))

}
