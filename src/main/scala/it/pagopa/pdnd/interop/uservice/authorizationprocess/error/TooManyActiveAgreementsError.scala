package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

final case class TooManyActiveAgreementsError(eserviceId: String, consumerId: String)
    extends Throwable(s"Too many active agreements were found for eservice($eserviceId)/consumer($consumerId)")
