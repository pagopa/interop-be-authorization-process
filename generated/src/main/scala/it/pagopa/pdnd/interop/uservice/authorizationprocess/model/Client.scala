package it.pagopa.pdnd.interop.uservice.authorizationprocess.model

import java.util.UUID

/** Models Client details
  *
  * @param id  for example: ''null''
  * @param eservice  for example: ''null''
  * @param consumer  for example: ''null''
  * @param agreement  for example: ''null''
  * @param name  for example: ''null''
  * @param purposes  for example: ''null''
  * @param description  for example: ''null''
  * @param status  for example: ''null''
  * @param operators  for example: ''null''
  */
final case class Client(
  id: UUID,
  eservice: EService,
  consumer: Organization,
  agreement: Agreement,
  name: String,
  purposes: String,
  description: Option[String],
  status: String,
  operators: Option[Seq[Operator]]
)
