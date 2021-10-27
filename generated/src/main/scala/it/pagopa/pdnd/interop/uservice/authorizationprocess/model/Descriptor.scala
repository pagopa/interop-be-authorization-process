package it.pagopa.pdnd.interop.uservice.authorizationprocess.model

import java.util.UUID

/**
 * Models an E-Service Descriptor
 *
 * @param id  for example: ''null''
 * @param status  for example: ''null''
 * @param version  for example: ''null''
*/
final case class Descriptor (
  id: UUID,
  status: String,
  version: String
)

