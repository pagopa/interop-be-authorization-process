package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

/** ADT modeling a not found descriptor error
  * @param eserviceId - identifier of the eservice to lookup
  * @param descriptorId - identifier of the descriptor to lookup
  */
final case class DescriptorNotFound(eserviceId: String, descriptorId: String)
    extends Throwable(s"Descriptor $descriptorId not found in eservice $eserviceId")
