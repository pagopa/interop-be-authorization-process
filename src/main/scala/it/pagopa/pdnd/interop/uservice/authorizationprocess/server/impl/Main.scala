package it.pagopa.pdnd.interop.uservice.authorizationprocess.server.impl

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.directives.SecurityDirectives
import akka.management.scaladsl.AkkaManagement
import com.bettercloud.vault.Vault
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.AuthApi
import it.pagopa.pdnd.interop.uservice.authorizationprocess.server.Controller
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.{AuthApiMarshallerImpl, AuthApiServiceImpl}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.system.{Authenticator, classicActorSystem}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.VaultService
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl.{
  JWTGeneratorImpl,
  JWTValidatorImpl,
  VaultServiceImpl
}
import kamon.Kamon

import scala.concurrent.Future

object Main extends App {

  Kamon.init()

  lazy val vault: Vault = getVaultClient

  val vaultService: VaultService     = new VaultServiceImpl(vault)
  val jwtValidator: JWTValidatorImpl = new JWTValidatorImpl(vaultService)
  val jwtGenerator: JWTGeneratorImpl = new JWTGeneratorImpl(vaultService)

  val authApi: AuthApi = new AuthApi(
    new AuthApiServiceImpl(vaultService, jwtValidator, jwtGenerator),
    new AuthApiMarshallerImpl(),
    SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
  )

  locally {
    val _ = AkkaManagement.get(classicActorSystem).start()
  }

  val controller: Controller = new Controller(authApi)

  val bindingFuture: Future[Http.ServerBinding] =
    Http().newServerAt("0.0.0.0", 8088).bind(controller.routes)

}