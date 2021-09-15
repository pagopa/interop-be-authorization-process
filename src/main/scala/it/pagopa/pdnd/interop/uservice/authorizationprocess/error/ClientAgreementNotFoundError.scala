package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

final case class ClientAgreementNotFoundError(clientId: String)
    extends Throwable(s"No agreement found for client $clientId")
