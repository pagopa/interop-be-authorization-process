package it.pagopa.pdnd.interop.uservice.partymanagement.service.impl

import com.auth0.jwt.JWT
import com.bettercloud.vault.Vault
import com.bettercloud.vault.response.LogicalResponse
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.ErrorOr
import it.pagopa.pdnd.interop.uservice.partymanagement.service.JWTGenerator

import scala.jdk.CollectionConverters.MapHasAsScala
import scala.util.Try

class JWTGeneratorImpl(vault: Vault) extends JWTGenerator {

  lazy val privateKey: Option[String] = getPrivateKey

  override def generate(algorithm: String): ErrorOr[String] = for {
    algorithm <- generateAlgorithm(algorithm, privateKey)
    token     <- Try(JWT.create.withIssuer("auth0").sign(algorithm)).toEither
  } yield token

  private def getPrivateKey: Option[String] = {
    val data: LogicalResponse = vault.logical().read("secret/data/pdnd-interop-dev/keys/pdnd/rsa")
    data.getData.asScala.get("privateKey")
  }
}
