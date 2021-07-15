package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import com.nimbusds.jwt.SignedJWT

import scala.concurrent.Future

trait JWTGenerator {
  def generate(jwt: SignedJWT): Future[String]
}
