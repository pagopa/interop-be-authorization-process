package it.pagopa.pdnd.interop.uservice.authorizationprocess.model

/** Models the seed for an Operator-Client relationship to be persisted
  *
  * @param taxCode  for example: ''null''
  * @param name  for example: ''null''
  * @param surname  for example: ''null''
  */
final case class OperatorSeed(taxCode: String, name: String, surname: String)
