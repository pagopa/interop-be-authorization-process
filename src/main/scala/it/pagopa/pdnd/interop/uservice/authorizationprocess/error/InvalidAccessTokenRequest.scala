package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

final case class InvalidAccessTokenRequest(errors: List[String]) extends Throwable("Invalid access token request")
