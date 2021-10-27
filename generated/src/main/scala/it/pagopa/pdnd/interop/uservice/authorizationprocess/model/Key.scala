package it.pagopa.pdnd.interop.uservice.authorizationprocess.model

/** Models a JWK
  *
  * @param kty  for example: ''null''
  * @param key_ops  for example: ''null''
  * @param use  for example: ''null''
  * @param alg  for example: ''null''
  * @param kid  for example: ''null''
  * @param x5u  for example: ''null''
  * @param x5t  for example: ''null''
  * @param x5tS256  for example: ''null''
  * @param x5c  for example: ''null''
  * @param crv  for example: ''null''
  * @param x  for example: ''null''
  * @param y  for example: ''null''
  * @param d  for example: ''null''
  * @param k  for example: ''null''
  * @param n  for example: ''null''
  * @param e  for example: ''null''
  * @param p  for example: ''null''
  * @param q  for example: ''null''
  * @param dp  for example: ''null''
  * @param dq  for example: ''null''
  * @param qi  for example: ''null''
  * @param oth  for example: ''null''
  */
final case class Key(
  kty: String,
  key_ops: Option[Seq[String]],
  use: Option[String],
  alg: Option[String],
  kid: String,
  x5u: Option[String],
  x5t: Option[String],
  x5tS256: Option[String],
  x5c: Option[Seq[String]],
  crv: Option[String],
  x: Option[String],
  y: Option[String],
  d: Option[String],
  k: Option[String],
  n: Option[String],
  e: Option[String],
  p: Option[String],
  q: Option[String],
  dp: Option[String],
  dq: Option[String],
  qi: Option[String],
  oth: Option[Seq[OtherPrimeInfo]]
)
