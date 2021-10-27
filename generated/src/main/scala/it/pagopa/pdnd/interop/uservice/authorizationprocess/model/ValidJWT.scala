package it.pagopa.pdnd.interop.uservice.authorizationprocess.model

import java.time.OffsetDateTime

/** Models a JWT payload
  *
  * @param iss  for example: ''null''
  * @param sub  for example: ''null''
  * @param aud  for example: ''null''
  * @param exp  for example: ''null''
  * @param nbf  for example: ''null''
  * @param iat  for example: ''null''
  * @param jti  for example: ''null''
  */
final case class ValidJWT(
  iss: String,
  sub: String,
  aud: Seq[String],
  exp: OffsetDateTime,
  nbf: OffsetDateTime,
  iat: OffsetDateTime,
  jti: String
)
