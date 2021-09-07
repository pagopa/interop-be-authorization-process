package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

final case class UuidConversionError(value: String) extends Throwable(s"Unable to convert $value to uuid")
