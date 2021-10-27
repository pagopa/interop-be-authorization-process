package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

import java.util.UUID

/** ADT modeling a not found descriptor error
  *
  * @param eserviceId - identifier of the eservice to lookup
  * @param descriptorId - identifier of the descriptor to lookup
  */
final case class DescriptorNotFound(eserviceId: UUID, descriptorId: UUID)
    extends Throwable(s"Descriptor ${descriptorId.toString} not found in eservice ${eserviceId.toString}")
