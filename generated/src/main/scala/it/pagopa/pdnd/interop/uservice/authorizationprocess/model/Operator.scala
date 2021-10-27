package it.pagopa.pdnd.interop.uservice.authorizationprocess.model

import java.util.UUID

/** Models a Client Operator
  *
  * @param id  for example: ''null''
  * @param taxCode  for example: ''null''
  * @param name  for example: ''null''
  * @param surname  for example: ''null''
  * @param role  for example: ''null''
  * @param platformRole  for example: ''null''
  * @param status  for example: ''null''
  */
final case class Operator(
  id: UUID,
  taxCode: String,
  name: String,
  surname: String,
  role: String,
  platformRole: String,
  status: String
)
