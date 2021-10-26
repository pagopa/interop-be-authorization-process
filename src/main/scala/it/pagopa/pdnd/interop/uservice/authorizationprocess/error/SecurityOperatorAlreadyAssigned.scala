package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

import java.util.UUID

final case class SecurityOperatorAlreadyAssigned(clientId: UUID, operatorTaxCode: String)
    extends Throwable(s"Security operator $operatorTaxCode is already assigned to the client ${clientId.toString}")
