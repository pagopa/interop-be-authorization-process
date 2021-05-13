package it.pagopa.pdnd.interop.uservice.partymanagement.service.impl

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.bettercloud.vault.Vault
import com.bettercloud.vault.response.LogicalResponse
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.decodeBase64
import it.pagopa.pdnd.interop.uservice.partymanagement.model.token.TokenSeed
import it.pagopa.pdnd.interop.uservice.partymanagement.service.JWTGenerator
import org.slf4j.{Logger, LoggerFactory}

import java.util.Date
import scala.jdk.CollectionConverters.MapHasAsScala
import scala.util.Try

class JWTGeneratorImpl(vault: Vault) extends JWTGenerator {
  private val logger: Logger       = LoggerFactory.getLogger(this.getClass)
  lazy val privateKey: Try[String] = getPrivateKey

  override def generate(seed: TokenSeed): Try[String] = for {
    pk        <- privateKey
    algorithm <- generateAlgorithm(seed.algorithm, pk)
    _ = logger.info("Generating token")
    token <- createToken(algorithm, seed)
    _ = logger.info("Token generated")
  } yield token

  private def createToken(algorithm: Algorithm, seed: TokenSeed): Try[String] = Try {
    JWT.create
      .withKeyId(seed.kid)
      .withSubject(seed.clientId)
      .withIssuer(seed.issuer)
      .withAudience(seed.audience)
      .withIssuedAt(new Date(seed.issuedAt))
      .withExpiresAt(new Date(seed.expireAt))
      .sign(algorithm)
  }

  private def getPrivateKey: Try[String] = {
    val data: LogicalResponse = vault.logical().read("secret/data/pdnd-interop-dev/keys/pdnd/rsa")
    data.getData.asScala
      .get("private")
      .map(decodeBase64)
      .toRight(new RuntimeException("PDND private key not found"))
      .toTry
  }
}
