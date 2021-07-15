package it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl

import com.nimbusds.jwt.SignedJWT
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.AccessTokenRequest
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{JWTValidator, KeyManager}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

final case class JWTValidatorImpl(keyManager: KeyManager)(implicit ex: ExecutionContext) extends JWTValidator {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def validate(accessTokenRequest: AccessTokenRequest): Future[SignedJWT] =
    for {
      info <- extractJwtInfo(accessTokenRequest)
      (jwt, clientId, kid) = info
      publicKey <- keyManager.getKey(clientId, kid)
      verifier  <- getVerifier(jwt.getHeader.getAlgorithm, publicKey)
      _ = logger.info("Verify signature")
      verified <- verify(verifier, jwt)
      _ = logger.info("Signature verified")
    } yield verified

}
