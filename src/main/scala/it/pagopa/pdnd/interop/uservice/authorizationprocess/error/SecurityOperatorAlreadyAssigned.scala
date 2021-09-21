package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

final case class SecurityOperatorAlreadyAssigned(clientId: String, operatorTaxCode: String)
    extends Throwable(s"Security operator $operatorTaxCode is already assigned to the client $clientId")
