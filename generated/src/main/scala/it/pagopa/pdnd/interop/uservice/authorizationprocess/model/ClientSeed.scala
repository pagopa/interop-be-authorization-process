package it.pagopa.pdnd.interop.uservice.authorizationprocess.model

import java.util.UUID

/** Client creation request body
  *
  * @param eServiceId  for example: ''null''
  * @param consumerId  for example: ''null''
  * @param name  for example: ''null''
  * @param purposes  for example: ''null''
  * @param description  for example: ''null''
  */
final case class ClientSeed(
  eServiceId: UUID,
  consumerId: UUID,
  name: String,
  purposes: String,
  description: Option[String]
)
