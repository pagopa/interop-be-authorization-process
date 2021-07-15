package it.pagopa.pdnd.interop.uservice.authorizationprocess.model.token

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.SignedJWT
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.utils.expireIn

import java.time.{Clock, Instant, ZoneId}
import java.util.UUID
import scala.util.Try

final case class TokenSeed(
  id: UUID,
  algorithm: JWSAlgorithm,
  kid: String,
  clientId: String,
  issuer: String,
  issuedAt: Long,
  expireAt: Long
)

object TokenSeed {
  def create(assertion: SignedJWT, key: JWK): Try[TokenSeed] = Try {
    TokenSeed(
      id = UUID.randomUUID(),
      algorithm = assertion.getHeader.getAlgorithm,
      kid = key.computeThumbprint().decodeToString(),
      clientId = assertion.getJWTClaimsSet.getSubject,
      issuer = "PDND-Interop",
      issuedAt = Instant.now(Clock.system(ZoneId.of("UTC"))).toEpochMilli,
      expireAt = Instant.now(Clock.system(ZoneId.of("UTC"))).plusMillis(expireIn).toEpochMilli
    )

  }
}
