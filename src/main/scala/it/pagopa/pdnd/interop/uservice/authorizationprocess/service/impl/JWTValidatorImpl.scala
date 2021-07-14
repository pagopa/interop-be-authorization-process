package it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.utils.decodeBase64
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.AccessTokenRequest
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{JWTValidator, VaultService}
import org.slf4j.{Logger, LoggerFactory}

import scala.util.Try

class JWTValidatorImpl(vaultService: VaultService) extends JWTValidator {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

//  TODO: use agreement_id
  override def validate(accessTokenRequest: AccessTokenRequest): Try[DecodedJWT] =
    for {
      jwt     <- Try(JWT.decode(accessTokenRequest.client_assertion))
      subject <- Try(jwt.getSubject)
      clientId <- Either
        .cond(
          subject == accessTokenRequest.client_id.map(_.toString).getOrElse(subject),
          subject,
          new RuntimeException(s"ClientId ${accessTokenRequest.client_id.toString} not equal to subject $subject")
        )
        .toTry
      _ = logger.info(clientId)
      kid <- Try(jwt.getKeyId)
      _ = logger.info(kid)
      publicKey <- getPublicKey(clientId, kid)
      algorithm <- generateAlgorithm(jwt.getAlgorithm, publicKey)
      verifier = JWT.require(algorithm).build()
      _        = logger.info("Verify signature")
      verified <- Try(verifier.verify(accessTokenRequest.client_assertion))
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
