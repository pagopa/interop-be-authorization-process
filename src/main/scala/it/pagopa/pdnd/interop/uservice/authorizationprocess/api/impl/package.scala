package it.pagopa.pdnd.interop.uservice.authorizationprocess.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import it.pagopa.pdnd.interop.commons.utils.SprayCommonFormats.{uuidFormat, offsetDateTimeFormat}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val problemFormat: RootJsonFormat[Problem] = jsonFormat3(Problem)
  implicit val clientCredentialsResponseFormat: RootJsonFormat[ClientCredentialsResponse] = jsonFormat3(
    ClientCredentialsResponse
  )
  implicit val organizationFormat: RootJsonFormat[Organization]         = jsonFormat2(Organization)
  implicit val descriptorFormat: RootJsonFormat[Descriptor]             = jsonFormat3(Descriptor)
  implicit val eServiceFormat: RootJsonFormat[EService]                 = jsonFormat4(EService)
  implicit val operatorFormat: RootJsonFormat[Operator]                 = jsonFormat7(Operator)
  implicit val agreementFormat: RootJsonFormat[Agreement]               = jsonFormat3(Agreement)
  implicit val clientSeedFormat: RootJsonFormat[ClientSeed]             = jsonFormat5(ClientSeed)
  implicit val clientFormat: RootJsonFormat[Client]                     = jsonFormat9(Client)
  implicit val primeInfoFormat: RootJsonFormat[OtherPrimeInfo]          = jsonFormat3(OtherPrimeInfo)
  implicit val keyFormat: RootJsonFormat[Key]                           = jsonFormat22(Key)
  implicit val clientKeyFormat: RootJsonFormat[ClientKey]               = jsonFormat1(ClientKey)
  implicit val encodedClientKeyFormat: RootJsonFormat[EncodedClientKey] = jsonFormat1(EncodedClientKey)
  implicit val clientKeysFormat: RootJsonFormat[ClientKeys]             = jsonFormat1(ClientKeys)
  implicit val keySeedFormat: RootJsonFormat[KeySeed]                   = jsonFormat4(KeySeed)
  implicit val operatorKeySeedFormat: RootJsonFormat[OperatorKeySeed]   = jsonFormat4(OperatorKeySeed)
  implicit val keysResponseFormat: RootJsonFormat[KeysResponse]         = jsonFormat1(KeysResponse)
  implicit val validJWTFormat: RootJsonFormat[ValidJWT]                 = jsonFormat7(ValidJWT)

}
