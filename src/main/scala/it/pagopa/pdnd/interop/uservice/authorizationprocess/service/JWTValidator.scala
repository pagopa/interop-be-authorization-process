package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import com.nimbusds.jwt.SignedJWT

import java.util.UUID
import scala.concurrent.Future

trait JWTValidator {
  def validate(
    clientAssertion: String,
    clientAssertionType: String,
    grantType: String,
    clientId: Option[UUID]
  ): Future[(String, SignedJWT)]
}
