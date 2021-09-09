package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

final case class EnumParameterError(fieldName: String, values: Seq[String])
    extends Throwable(s"Invalid parameter value. parameter '$fieldName' should be in ${values.mkString("[", ",", "]")}")
