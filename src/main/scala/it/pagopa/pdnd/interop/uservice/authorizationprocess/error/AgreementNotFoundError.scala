package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

import java.util.UUID

final case class AgreementNotFoundError(eserviceId: UUID, consumerId: UUID)
    extends Throwable(
      s"No active agreement was found for eservice/consumer. ${eserviceId.toString}/${consumerId.toString}"
    )
