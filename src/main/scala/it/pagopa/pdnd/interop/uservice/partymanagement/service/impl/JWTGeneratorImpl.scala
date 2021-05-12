package it.pagopa.pdnd.interop.uservice.partymanagement.service.impl

import com.auth0.jwt.JWT
import com.bettercloud.vault.Vault
import com.bettercloud.vault.response.LogicalResponse
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.{ErrorOr, decodeBase64}
import it.pagopa.pdnd.interop.uservice.partymanagement.service.JWTGenerator

import scala.jdk.CollectionConverters.MapHasAsScala
import scala.util.Try

class JWTGeneratorImpl(vault: Vault) extends JWTGenerator {

  lazy val privateKey: ErrorOr[String] = getPrivateKey

  override def generate(algorithm: String): ErrorOr[String] = for {
    pk        <- privateKey
    algorithm <- generateAlgorithm(algorithm, pk)
    token     <- Try(JWT.create.withIssuer("auth0").sign(algorithm)).toEither
  } yield token

  private def getPrivateKey: ErrorOr[String] = {
    val data: LogicalResponse = vault.logical().read("secret/data/pdnd-interop-dev/keys/pdnd/rsa")
    data.getData.asScala.get("private").map(decodeBase64).toRight(new RuntimeException("PDND private key not found"))
  }
}
