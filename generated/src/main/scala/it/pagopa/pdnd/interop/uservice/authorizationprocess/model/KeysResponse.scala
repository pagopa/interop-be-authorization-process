package it.pagopa.pdnd.interop.uservice.authorizationprocess.model


/**
 * JWKS
 *
 * @param keys  for example: ''null''
*/
final case class KeysResponse (
  keys: Seq[Key]
)

