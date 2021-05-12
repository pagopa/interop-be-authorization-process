package it.pagopa.pdnd.interop.uservice.partymanagement.service

import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.ErrorOr

trait JWTGenerator {
  def generate(algorithm: String): ErrorOr[String]
}
