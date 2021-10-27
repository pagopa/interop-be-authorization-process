package it.pagopa.pdnd.interop.uservice.authorizationprocess.model


/**
 * @param access_token  for example: ''null''
 * @param token_type  for example: ''null''
 * @param expires_in  for example: ''null''
*/
final case class ClientCredentialsResponse (
  access_token: String,
  token_type: String,
  expires_in: Long
)

