package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

import java.util.UUID

final case class UnknownAgreementDescriptor(agreementId: UUID, eServiceId: UUID, descriptorId: UUID)
    extends Throwable(
      s"Unable to find descriptor ${descriptorId.toString} in E-Service ${eServiceId.toString} for agreement ${agreementId.toString}"
    )
