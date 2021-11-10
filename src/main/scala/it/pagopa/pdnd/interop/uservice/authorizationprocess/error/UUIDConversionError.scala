package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

final case class UUIDConversionError(value: String) extends Throwable(s"Unable to convert $value to uuid")
