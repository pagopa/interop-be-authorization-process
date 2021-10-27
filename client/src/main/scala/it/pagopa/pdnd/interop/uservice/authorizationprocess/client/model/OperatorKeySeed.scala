/**
 * Security Process Micro Service
 * This service is the security supplier
 *
 * The version of the OpenAPI document: {{version}}
 * Contact: support@example.com
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package it.pagopa.pdnd.interop.uservice.authorizationprocess.client.model

import it.pagopa.pdnd.interop.uservice.authorizationprocess.client.invoker.ApiModel

case class OperatorKeySeed (
  /* Represents the identifier of the client related to the key */
  clientId: String,
  /* Base64 UTF-8 encoding of a public key in PEM format */
  key: String,
  /* The expected use for this key. */
  use: OperatorKeySeedEnums.Use,
  /* The algorithm type of the key. */
  alg: String
) extends ApiModel

object OperatorKeySeedEnums {

  type Use = Use.Value
  object Use extends Enumeration {
    val Sig = Value("sig")
    val Enc = Value("enc")
  }

}

