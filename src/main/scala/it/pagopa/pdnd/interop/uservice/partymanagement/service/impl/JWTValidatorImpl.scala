package it.pagopa.pdnd.interop.uservice.partymanagement.service.impl

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.decodeBase64
import it.pagopa.pdnd.interop.uservice.partymanagement.service.{JWTValidator, VaultService}
import org.slf4j.{Logger, LoggerFactory}

import scala.util.Try

class JWTValidatorImpl(vaultService: VaultService) extends JWTValidator {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

//  TODO: use agreement_id
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

  // TODO use jwks service with auth0-jwks
  def getPublicKey(clientId: String, kid: String): Try[String] = {
    vaultService
      .getSecret(s"secret/data/pdnd-interop-dev/keys/organizations/$clientId/keys/$kid")
      .get("public")
      .map(decodeBase64)
      .toRight(new RuntimeException(s"Public key $kid not found for $clientId"))
      .toTry
  }
}
