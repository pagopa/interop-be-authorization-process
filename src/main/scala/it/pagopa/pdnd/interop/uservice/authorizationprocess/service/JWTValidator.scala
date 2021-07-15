package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import com.nimbusds.jwt.SignedJWT
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.AccessTokenRequest

import scala.concurrent.Future

trait JWTValidator {
  def validate(accessTokenRequest: AccessTokenRequest): Future[SignedJWT]
}
