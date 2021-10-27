package it.pagopa.pdnd.interop.uservice.authorizationprocess.model


/**
 * Models an Organization
 *
 * @param institutionId  for example: ''null''
 * @param description  for example: ''null''
*/
final case class Organization (
  institutionId: String,
  description: String
)

