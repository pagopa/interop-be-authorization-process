package it.pagopa.interop.authorizationprocess.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.commons.utils.SprayCommonFormats.{offsetDateTimeFormat, uuidFormat}
import it.pagopa.interop.commons.utils.errors.ServiceCode
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val problemErrorFormat: RootJsonFormat[ProblemError]           = jsonFormat2(ProblemError)
  implicit val problemFormat: RootJsonFormat[Problem]                     = jsonFormat6(Problem)
  implicit val organizationFormat: RootJsonFormat[Organization]           = jsonFormat2(Organization)
  implicit val operatorProductFormat: RootJsonFormat[RelationshipProduct] = jsonFormat3(RelationshipProduct)
  implicit val operatorFormat: RootJsonFormat[Operator]                   = jsonFormat7(Operator)
  implicit val clientSeedFormat: RootJsonFormat[ClientSeed]               = jsonFormat3(ClientSeed)
  implicit val primeInfoFormat: RootJsonFormat[OtherPrimeInfo]            = jsonFormat3(OtherPrimeInfo)
  implicit val keyFormat: RootJsonFormat[Key]                             = jsonFormat7(Key)
  implicit val keysFormat: RootJsonFormat[Keys]                           = jsonFormat1(Keys)
  implicit val keySeedFormat: RootJsonFormat[KeySeed]                     = jsonFormat4(KeySeed)

  implicit val eServiceDescriptorFormat: RootJsonFormat[EServiceDescriptor]         = jsonFormat2(EServiceDescriptor)
  implicit val eServiceFormat: RootJsonFormat[EService]                             = jsonFormat2(EService)
  implicit val agreementFormat: RootJsonFormat[Agreement]                           = jsonFormat3(Agreement)
  implicit val clientAgreementDetailsFormat: RootJsonFormat[ClientAgreementDetails] =
    jsonFormat4(ClientAgreementDetails)

  implicit val clientEServiceDetailsFormat: RootJsonFormat[ClientEServiceDetails] = jsonFormat5(ClientEServiceDetails)
  implicit val clientPurposeDetailsFormat: RootJsonFormat[ClientPurposeDetails]   = jsonFormat3(ClientPurposeDetails)
  implicit val clientStatesChainFormat: RootJsonFormat[ClientStatesChain]         = jsonFormat4(ClientStatesChain)
  implicit val clientPurposeFormat: RootJsonFormat[ClientPurpose]                 = jsonFormat1(ClientPurpose)
  implicit val purposeFormat: RootJsonFormat[Purpose]                             = jsonFormat4(Purpose)
  implicit val purposeAddDetailsFormat: RootJsonFormat[PurposeAdditionDetails]    = jsonFormat1(PurposeAdditionDetails)

  implicit val clientFormat: RootJsonFormat[Client]   = jsonFormat8(Client)
  implicit val clientsFormat: RootJsonFormat[Clients] = jsonFormat2(Clients)

  implicit val clientEntryKeyFormat: RootJsonFormat[ClientWithKeys] = jsonFormat2(ClientWithKeys)
  implicit val clientsKeysFormat: RootJsonFormat[ClientsWithKeys]   = jsonFormat2(ClientsWithKeys)

  final val entityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]

  implicit val serviceCode: ServiceCode = ServiceCode("007")

}
