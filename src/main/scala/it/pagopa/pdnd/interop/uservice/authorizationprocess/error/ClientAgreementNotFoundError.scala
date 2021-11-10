package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

import java.util.UUID

final case class ClientAgreementNotFoundError(clientId: UUID)
    extends Throwable(s"No agreement found for client ${clientId.toString}")
