package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import com.auth0.jwt.interfaces.DecodedJWT
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.AccessTokenRequest

import scala.util.Try

trait JWTValidator {
  def validate(accessTokenRequest: AccessTokenRequest): Try[DecodedJWT]
}
