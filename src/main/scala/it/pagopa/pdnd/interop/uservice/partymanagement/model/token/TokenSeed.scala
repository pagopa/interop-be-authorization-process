package it.pagopa.pdnd.interop.uservice.partymanagement.model.token

import com.auth0.jwt.interfaces.DecodedJWT
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.expireIn

import java.time.{Clock, Instant, ZoneId}
import scala.jdk.CollectionConverters.ListHasAsScala
import scala.util.Try

final case class TokenSeed(
  algorithm: String,
  kid: String,
  clientId: String,
  issuer: String,
  issuedAt: Long,
  expireAt: Long,
  audience: String
)

object TokenSeed {
  def create(assertion: DecodedJWT): Try[TokenSeed] = {

    val audience = assertion.getAudience.asScala.headOption

    audience
      .map(aud =>
        TokenSeed(
          algorithm = assertion.getAlgorithm,
          kid = "PDND-Interop-Kid",
          clientId = assertion.getSubject,
          issuer = "PDND-Interop",
          issuedAt = Instant.now(Clock.system(ZoneId.of("UTC"))).toEpochMilli,
          expireAt = Instant.now(Clock.system(ZoneId.of("UTC"))).plusMillis(expireIn).toEpochMilli,
          audience = aud
        )
      )
      .toRight(new RuntimeException("Invalid audience"))
      .toTry

  }
}
