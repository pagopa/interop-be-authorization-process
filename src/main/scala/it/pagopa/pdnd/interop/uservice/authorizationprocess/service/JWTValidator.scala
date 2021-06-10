package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import com.auth0.jwt.interfaces.DecodedJWT

import scala.util.Try

trait JWTValidator {
  def validate(jwt: String): Try[DecodedJWT]
}
