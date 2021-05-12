package it.pagopa.pdnd.interop.uservice.partymanagement.service

import com.auth0.jwt.interfaces.DecodedJWT
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.ErrorOr

trait JWTValidator {
  def validate(jwt: String): ErrorOr[DecodedJWT]
}
