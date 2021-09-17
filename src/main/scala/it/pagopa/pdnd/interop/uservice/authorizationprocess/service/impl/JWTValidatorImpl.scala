package it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl

import com.nimbusds.jwt.SignedJWT
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.utils.{EitherOps, toUuid}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.error.KeyNotActiveError
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.AccessTokenRequest
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{AuthorizationManagementService, JWTValidator}
import it.pagopa.pdnd.interop.uservice.keymanagement.client.model.{ClientKey, ClientKeyEnums}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

final case class JWTValidatorImpl(keyManager: AuthorizationManagementService)(implicit ex: ExecutionContext)
    extends JWTValidator {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def validate(accessTokenRequest: AccessTokenRequest): Future[(String, SignedJWT)] =
    for {
      info <- extractJwtInfo(accessTokenRequest)
      (jwt, kid, clientId) = info
      clientUUid <- toUuid(clientId).toFuture
      publicKey  <- keyManager.getKey(clientUUid, kid)
      _          <- verifyKeyActivation(publicKey)
      verifier   <- getVerifier(jwt.getHeader.getAlgorithm, publicKey.key)
      _ = logger.info("Verify signature")
      verified <- verify(verifier, jwt)
      _ = logger.info("Signature verified")
    } yield clientId -> verified

  private[this] def verifyKeyActivation(clientKey: ClientKey): Future[Unit] =
    clientKey.status match {
      case ClientKeyEnums.Status.Active => Future.successful(())
      case _                            => Future.failed(KeyNotActiveError(clientKey.key.kid))
    }

}
