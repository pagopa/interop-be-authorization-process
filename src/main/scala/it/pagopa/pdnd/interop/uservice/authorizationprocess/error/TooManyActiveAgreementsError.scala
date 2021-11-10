package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

import java.util.UUID

final case class TooManyActiveAgreementsError(eserviceId: UUID, consumerId: UUID)
    extends Throwable(
      s"Too many active agreements were found for eservice(${eserviceId.toString})/consumer(${consumerId.toString})"
    )
