package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

final case class AgreementNotFoundError(eserviceId: String, consumerId: String)
    extends Throwable(s"No active agreement was found for eservice/consumer. $eserviceId/$consumerId")
