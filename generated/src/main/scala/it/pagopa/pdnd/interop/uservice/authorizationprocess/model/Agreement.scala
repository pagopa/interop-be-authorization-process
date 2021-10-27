package it.pagopa.pdnd.interop.uservice.authorizationprocess.model

import java.util.UUID

/**
 * Models an Agreement
 *
 * @param id  for example: ''null''
 * @param status  for example: ''null''
 * @param descriptor  for example: ''null''
*/
final case class Agreement (
  id: UUID,
  status: String,
  descriptor: Descriptor
)

