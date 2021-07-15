package it.pagopa.pdnd.interop.uservice.authorizationprocess.server.impl

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.management.scaladsl.AkkaManagement
import com.bettercloud.vault.Vault
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.AuthApi
import it.pagopa.pdnd.interop.uservice.authorizationprocess.server.Controller
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.{AuthApiMarshallerImpl, AuthApiServiceImpl}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.{ApplicationConfiguration, CorsSupport}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.system.{
  Authenticator,
  classicActorSystem,
  executionContext
}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{KeyManager, VaultService}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl.{
  JWTGeneratorImpl,
  JWTValidatorImpl,
  KeyManagerImpl,
  VaultServiceImpl
}
import it.pagopa.pdnd.interop.uservice.keymanagement.client.api.KeyApi
import it.pagopa.pdnd.interop.uservice.keymanagement.client.invoker.ApiInvoker
import kamon.Kamon

import scala.concurrent.Future

object Main extends App with CorsSupport {

  Kamon.init()

  lazy val vault: Vault = getVaultClient

  val invoker: ApiInvoker            = ApiInvoker()
  val keyApi: KeyApi                 = KeyApi(ApplicationConfiguration.getKeyManagementUrl)
  val keyManager: KeyManager         = KeyManagerImpl(invoker, keyApi)
  val vaultService: VaultService     = VaultServiceImpl(vault)
  val jwtValidator: JWTValidatorImpl = JWTValidatorImpl(keyManager)
  val jwtGenerator: JWTGeneratorImpl = JWTGeneratorImpl(vaultService)

  val authApi: AuthApi = new AuthApi(
    new AuthApiServiceImpl(jwtValidator, jwtGenerator),
    new AuthApiMarshallerImpl(),
    SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
  )

  locally {
    val _ = AkkaManagement.get(classicActorSystem).start()
  }

  val controller: Controller = new Controller(authApi)

  val bindingFuture: Future[Http.ServerBinding] =
    Http().newServerAt("0.0.0.0", 8088).bind(corsHandler(controller.routes))

}
