package it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl

import com.nimbusds.jwt.SignedJWT
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.utils.{EitherOps, toUuid}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.AccessTokenRequest
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{AuthorizationManagementService, JWTValidator}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

final case class JWTValidatorImpl(keyManager: AuthorizationManagementService)(implicit ex: ExecutionContext)
    extends JWTValidator {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def validate(accessTokenRequest: AccessTokenRequest): Future[SignedJWT] =
    for {
      info <- extractJwtInfo(accessTokenRequest)
      (jwt, kid, clientId) = info
      clientUUid <- toUuid(clientId).toFuture
      publicKey  <- keyManager.getKey(clientUUid, kid)
      verifier   <- getVerifier(jwt.getHeader.getAlgorithm, publicKey)
      _ = logger.info("Verify signature")
      verified <- verify(verifier, jwt)
      _ = logger.info("Signature verified")
    } yield verified

}
