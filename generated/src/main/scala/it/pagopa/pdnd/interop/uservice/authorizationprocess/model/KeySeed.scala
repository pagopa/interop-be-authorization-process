package it.pagopa.pdnd.interop.uservice.authorizationprocess.model

import java.util.UUID

/** Models the seed for a public key to be persisted
  *
  * @param operatorId Represents the identifier of the platform security operator that is uploading the key for example: ''null''
  * @param key Base64 UTF-8 encoding of a public key in PEM format for example: ''null''
  * @param use The expected use for this key. for example: ''null''
  * @param alg The algorithm type of the key. for example: ''null''
  */
final case class KeySeed(operatorId: UUID, key: String, use: String, alg: String)
