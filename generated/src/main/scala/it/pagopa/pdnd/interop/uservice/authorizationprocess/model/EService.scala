package it.pagopa.pdnd.interop.uservice.authorizationprocess.model

import java.util.UUID

/**
 * Models an E-Service
 *
 * @param id  for example: ''null''
 * @param name  for example: ''null''
 * @param provider  for example: ''null''
 * @param activeDescriptor  for example: ''null''
*/
final case class EService (
  id: UUID,
  name: String,
  provider: Organization,
  activeDescriptor: Option[Descriptor]
)

