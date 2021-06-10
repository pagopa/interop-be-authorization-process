package it.pagopa.pdnd.interop.uservice.authorizationprocess.model.token

import com.auth0.jwt.interfaces.DecodedJWT
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.utils.expireIn

import java.time.{Clock, Instant, ZoneId}
import java.util.UUID

final case class TokenSeed(
  id: UUID,
  algorithm: String,
  kid: String,
  clientId: String,
  issuer: String,
  issuedAt: Long,
  expireAt: Long
)

object TokenSeed {
  def create(assertion: DecodedJWT): TokenSeed = {
    TokenSeed(
      id = UUID.randomUUID(),
      algorithm = assertion.getAlgorithm,
      kid = "PDND-Interop-Kid",
      clientId = assertion.getSubject,
      issuer = "PDND-Interop",
      issuedAt = Instant.now(Clock.system(ZoneId.of("UTC"))).toEpochMilli,
      expireAt = Instant.now(Clock.system(ZoneId.of("UTC"))).plusMillis(expireIn).toEpochMilli
    )

  }
}
