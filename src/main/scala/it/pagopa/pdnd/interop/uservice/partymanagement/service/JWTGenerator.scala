package it.pagopa.pdnd.interop.uservice.partymanagement.service

import it.pagopa.pdnd.interop.uservice.partymanagement.model.token.TokenSeed

import scala.util.Try

trait JWTGenerator {
  def generate(seed: TokenSeed): Try[String]
}
