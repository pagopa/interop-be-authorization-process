package it.pagopa.interop.authorizationprocess.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Route
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.commons.jwt.{authorizeInterop, hasPermissions}
import it.pagopa.interop.commons.utils.SprayCommonFormats.{offsetDateTimeFormat, uuidFormat}
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.OperationForbidden
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val problemErrorFormat: RootJsonFormat[ProblemError]           = jsonFormat2(ProblemError)
  implicit val problemFormat: RootJsonFormat[Problem]                     = jsonFormat5(Problem)
  implicit val organizationFormat: RootJsonFormat[Organization]           = jsonFormat2(Organization)
  implicit val operatorProductFormat: RootJsonFormat[RelationshipProduct] = jsonFormat3(RelationshipProduct)
  implicit val operatorFormat: RootJsonFormat[Operator]                   = jsonFormat7(Operator)
  implicit val clientSeedFormat: RootJsonFormat[ClientSeed]               = jsonFormat3(ClientSeed)
  implicit val primeInfoFormat: RootJsonFormat[OtherPrimeInfo]            = jsonFormat3(OtherPrimeInfo)
  implicit val keyFormat: RootJsonFormat[Key]                             = jsonFormat22(Key)
  implicit val clientKeyFormat: RootJsonFormat[ClientKey]                 = jsonFormat3(ClientKey)
  implicit val encodedClientKeyFormat: RootJsonFormat[EncodedClientKey]   = jsonFormat1(EncodedClientKey)
  implicit val clientKeysFormat: RootJsonFormat[ClientKeys]               = jsonFormat1(ClientKeys)
  implicit val keySeedFormat: RootJsonFormat[KeySeed]                     = jsonFormat4(KeySeed)
  implicit val OperatorDetailsFormat: RootJsonFormat[OperatorDetails]     = jsonFormat3(OperatorDetails)
  implicit val readClientKeyFormat: RootJsonFormat[ReadClientKey]         = jsonFormat4(ReadClientKey)
  implicit val readClientKeysFormat: RootJsonFormat[ReadClientKeys]       = jsonFormat1(ReadClientKeys)

  implicit val eServiceDescriptorFormat: RootJsonFormat[EServiceDescriptor]         = jsonFormat2(EServiceDescriptor)
  implicit val eServiceFormat: RootJsonFormat[EService]                             = jsonFormat2(EService)
  implicit val agreementFormat: RootJsonFormat[Agreement]                           = jsonFormat3(Agreement)
  implicit val clientAgreementDetailsFormat: RootJsonFormat[ClientAgreementDetails] =
    jsonFormat3(ClientAgreementDetails)
  implicit val clientEServiceDetailsFormat: RootJsonFormat[ClientEServiceDetails]   = jsonFormat4(ClientEServiceDetails)
  implicit val clientPurposeDetailsFormat: RootJsonFormat[ClientPurposeDetails]     = jsonFormat2(ClientPurposeDetails)
  implicit val clientStatesChainFormat: RootJsonFormat[ClientStatesChain]           = jsonFormat4(ClientStatesChain)
  implicit val purposeFormat: RootJsonFormat[Purpose]                               = jsonFormat4(Purpose)
  implicit val purposeAddDetailsFormat: RootJsonFormat[PurposeAdditionDetails] = jsonFormat1(PurposeAdditionDetails)

  implicit val clientFormat: RootJsonFormat[Client]   = jsonFormat7(Client)
  implicit val clientsFormat: RootJsonFormat[Clients] = jsonFormat1(Clients)

  final val entityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]

  final val serviceErrorCodePrefix: String = "007"
  final val defaultProblemType: String     = "about:blank"
  final val defaultErrorMessage: String    = "Unknown error"

  def problemOf(httpError: StatusCode, error: ComponentError): Problem =
    Problem(
      `type` = defaultProblemType,
      status = httpError.intValue,
      title = httpError.defaultMessage,
      errors = Seq(
        ProblemError(
          code = s"$serviceErrorCodePrefix-${error.code}",
          detail = Option(error.getMessage).getOrElse(defaultErrorMessage)
        )
      )
    )

  def problemOf(httpError: StatusCode, errors: List[ComponentError]): Problem =
    Problem(
      `type` = defaultProblemType,
      status = httpError.intValue,
      title = httpError.defaultMessage,
      errors = errors.map(error =>
        ProblemError(
          code = s"$serviceErrorCodePrefix-${error.code}",
          detail = Option(error.getMessage).getOrElse(defaultErrorMessage)
        )
      )
    )

  private[impl] def authorize(roles: String*)(
    route: => Route
  )(implicit contexts: Seq[(String, String)], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    authorizeInterop(hasPermissions(roles: _*), problemOf(StatusCodes.Forbidden, OperationForbidden)) {
      route
    }

}
