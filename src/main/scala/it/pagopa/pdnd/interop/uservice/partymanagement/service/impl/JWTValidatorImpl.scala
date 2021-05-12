package it.pagopa.pdnd.interop.uservice.partymanagement.service.impl

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import com.bettercloud.vault.Vault
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.ErrorOr
import it.pagopa.pdnd.interop.uservice.partymanagement.service.JWTValidator

import scala.jdk.CollectionConverters.MapHasAsScala
import scala.util.Try

class JWTValidatorImpl(vault: Vault) extends JWTValidator {

  override def validate(token: String): ErrorOr[DecodedJWT] =
    for {
      jwt      <- Try(JWT.decode(token)).toEither
      clientId <- Try(jwt.getClaim("iss").asString).toEither
      kid      <- Try(jwt.getKeyId).toEither
      publicKey = getPublicKey(clientId, kid)
      algorithm <- generateAlgorithm(jwt.getAlgorithm, publicKey)
      verifier = JWT.require(algorithm).build()
      verified <- Try(verifier.verify(token)).toEither
    } yield verified

  def getPublicKey(clientId: String, kid: String): Option[String] = {
    val data = vault.logical().read(s"secret/data/pdnd-interop-dev/keys/organizations/$clientId/keys/$kid")
    data.getData.asScala.get("publicKey")
  }
}
