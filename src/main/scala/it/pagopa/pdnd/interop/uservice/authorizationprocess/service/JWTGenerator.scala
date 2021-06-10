package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.token.TokenSeed

import scala.util.Try

trait JWTGenerator {
  def generate(seed: TokenSeed): Try[String]
}
