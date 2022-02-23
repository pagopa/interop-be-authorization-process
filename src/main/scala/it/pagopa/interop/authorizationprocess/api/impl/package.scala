package it.pagopa.interop.authorizationprocess.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCode
import it.pagopa.interop.authorizationprocess.model._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import it.pagopa.interop.commons.utils.SprayCommonFormats.{offsetDateTimeFormat, uuidFormat}
import it.pagopa.interop.commons.utils.errors.ComponentError

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val problemErrorFormat: RootJsonFormat[ProblemError] = jsonFormat2(ProblemError)
  implicit val problemFormat: RootJsonFormat[Problem]           = jsonFormat5(Problem)
  implicit val clientCredentialsResponseFormat: RootJsonFormat[ClientCredentialsResponse] = jsonFormat3(
    ClientCredentialsResponse
  )
  implicit val organizationFormat: RootJsonFormat[Organization]           = jsonFormat2(Organization)
  implicit val operatorProductFormat: RootJsonFormat[RelationshipProduct] = jsonFormat3(RelationshipProduct)
  implicit val operatorFormat: RootJsonFormat[Operator]                   = jsonFormat7(Operator)
  implicit val clientSeedFormat: RootJsonFormat[ClientSeed]               = jsonFormat3(ClientSeed)
  implicit val primeInfoFormat: RootJsonFormat[OtherPrimeInfo]            = jsonFormat3(OtherPrimeInfo)
  implicit val keyFormat: RootJsonFormat[Key]                             = jsonFormat22(Key)
  implicit val clientKeyFormat: RootJsonFormat[ClientKey]                 = jsonFormat3(ClientKey)
  implicit val encodedClientKeyFormat: RootJsonFormat[EncodedClientKey]   = jsonFormat1(EncodedClientKey)
  implicit val clientKeysFormat: RootJsonFormat[ClientKeys]               = jsonFormat1(ClientKeys)
  implicit val keySeedFormat: RootJsonFormat[KeySeed]                     = jsonFormat5(KeySeed)
  implicit val operatorKeySeedFormat: RootJsonFormat[OperatorKeySeed]     = jsonFormat5(OperatorKeySeed)

  implicit val clientAgreementDetailsFormat: RootJsonFormat[ClientAgreementDetails] =
    jsonFormat2(ClientAgreementDetails)
  implicit val clientEServiceDetailsFormat: RootJsonFormat[ClientEServiceDetails] = jsonFormat4(ClientEServiceDetails)
  implicit val clientPurposeDetailsFormat: RootJsonFormat[ClientPurposeDetails]   = jsonFormat2(ClientPurposeDetails)
  implicit val clientStatesChainFormat: RootJsonFormat[ClientStatesChain]         = jsonFormat4(ClientStatesChain)
  implicit val purposeFormat: RootJsonFormat[Purpose]                             = jsonFormat2(Purpose)
  implicit val purposeAddDetailsFormat: RootJsonFormat[PurposeAdditionDetails]    = jsonFormat1(PurposeAdditionDetails)

  implicit val clientFormat: RootJsonFormat[Client]   = jsonFormat7(Client)
  implicit val clientsFormat: RootJsonFormat[Clients] = jsonFormat1(Clients)

  final val serviceErrorCodePrefix: String = "007"
  final val defaultProblemType: String     = "about:blank"

  def problemOf(httpError: StatusCode, error: ComponentError, defaultMessage: String = "Unknown error"): Problem =
    Problem(
      `type` = defaultProblemType,
      status = httpError.intValue,
      title = httpError.defaultMessage,
      errors = Seq(
        ProblemError(
          code = s"$serviceErrorCodePrefix-${error.code}",
          detail = Option(error.getMessage).getOrElse(defaultMessage)
        )
      )
    )
}
