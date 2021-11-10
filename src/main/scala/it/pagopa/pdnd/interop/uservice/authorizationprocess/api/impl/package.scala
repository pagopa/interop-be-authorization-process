package it.pagopa.pdnd.interop.uservice.authorizationprocess.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.utils.uuidFormat
import it.pagopa.pdnd.interop.uservice.authorizationprocess.error.UnauthenticatedError
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, RootJsonFormat, deserializationError}

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter
import scala.concurrent.Future
import scala.util.Try

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val localTimeFormat: JsonFormat[OffsetDateTime] = new JsonFormat[OffsetDateTime] {

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val deserializationErrorMessage =
      s"Expected date time in ISO offset date time format ex. ${OffsetDateTime.now().format(formatter)}"

    override def write(obj: OffsetDateTime): JsValue = JsString(formatter.format(obj))

    override def read(json: JsValue): OffsetDateTime = {
      json match {
        case JsString(lTString) =>
          Try(OffsetDateTime.of(LocalDateTime.parse(lTString, formatter), ZoneOffset.UTC))
            .getOrElse(deserializationError(deserializationErrorMessage))
        case _ => deserializationError(deserializationErrorMessage)
      }
    }
  }

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

  def extractBearer(contexts: Seq[(String, String)]): Future[String] = Future.fromTry {
    contexts.toMap.get("bearer").toRight(UnauthenticatedError).toTry
  }
}
