package it.pagopa.pdnd.interop.uservice.partymanagement.service.impl

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import com.bettercloud.vault.Vault
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.decodeBase64
import it.pagopa.pdnd.interop.uservice.partymanagement.service.JWTValidator
import org.slf4j.{Logger, LoggerFactory}

import scala.jdk.CollectionConverters.MapHasAsScala
import scala.util.Try

class JWTValidatorImpl(vault: Vault) extends JWTValidator {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)
  override def validate(token: String): Try[DecodedJWT] =
    for {
      jwt      <- Try(JWT.decode(token))
      clientId <- Try(jwt.getClaim("iss").asString)
      _ = logger.info(clientId)
      kid <- Try(jwt.getKeyId)
      _ = logger.info(kid)
      publicKey <- getPublicKey(clientId, kid)
      algorithm <- generateAlgorithm(jwt.getAlgorithm, publicKey)
      verifier = JWT.require(algorithm).build()
      _        = logger.info("Verify signature")
      verified <- Try(verifier.verify(token))
      _ = logger.info("Signature verified")
    } yield verified

  def getPublicKey(clientId: String, kid: String): Try[String] = {
    val data = vault.logical().read(s"secret/data/pdnd-interop-dev/keys/organizations/$clientId/keys/$kid")
    data.getData.asScala
      .get("public")
      .map(decodeBase64)
      .toRight(new RuntimeException(s"Public key $kid not found for $clientId"))
      .toTry
  }
}
