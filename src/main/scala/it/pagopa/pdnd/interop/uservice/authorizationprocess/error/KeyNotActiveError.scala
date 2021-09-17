package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

final case class KeyNotActiveError(kid: String) extends Throwable(s"Key with kid $kid is not active")
