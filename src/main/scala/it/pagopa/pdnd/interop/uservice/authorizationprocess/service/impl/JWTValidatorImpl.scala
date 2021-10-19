package it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl

import com.nimbusds.jose.{JWSAlgorithm, JWSVerifier}
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.system.TryOps
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.utils.{EitherOps, toUuid}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.error.KeyNotActiveError
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{
  AuthorizationManagementService,
  JWTValidator,
  VaultService
}
import it.pagopa.pdnd.interop.uservice.keymanagement.client.model.{ClientKey, ClientKeyEnums}
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

final case class JWTValidatorImpl(keyManager: AuthorizationManagementService, vaultService: VaultService)(implicit
  ex: ExecutionContext
) extends JWTValidator {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def validate(
    clientAssertion: String,
    clientAssertionType: String,
    grantType: String,
    clientId: Option[UUID]
  ): Future[(String, SignedJWT)] =
    for {
      info <- extractJwtInfo(clientAssertion, clientAssertionType, grantType, clientId)
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

  override def validateBearer(bearer: String): Future[JWTClaimsSet] = {
    for {
      jwt       <- Try(SignedJWT.parse(bearer)).toFuture
      algorithm <- Try(jwt.getHeader.getAlgorithm).toFuture
      kid       <- Try(jwt.getHeader.getKeyID).toFuture
      verifier  <- getPublicKey(algorithm, kid)
      _ = logger.info("Verify bearer")
      _ <- verify(verifier, jwt)
      _ = logger.info("Bearer verified")
    } yield jwt.getJWTClaimsSet
  }

  private def getPublicKey(algorithm: JWSAlgorithm, kid: String): Future[JWSVerifier] = algorithm match {
    case JWSAlgorithm.RS256 | JWSAlgorithm.RS384 | JWSAlgorithm.RS512 =>
      VaultService
        .extractKeyPath("rsa", "public")
        .map(vaultService.getSecret(kid))
        .map(key => rsa(key))
        .toFuture
        .flatten
    case JWSAlgorithm.ES256 =>
      VaultService.extractKeyPath("ec", "public").map(vaultService.getSecret(kid)).map(key => ec(key)).toFuture.flatten
    case _ => Future.failed(new RuntimeException("Invalid key algorithm"))

  }
}
