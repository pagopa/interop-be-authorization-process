package it.pagopa.pdnd.interop.uservice.partymanagement.common

import spray.json.{JsString, JsValue, JsonFormat, deserializationError}

import java.net.URI
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.{Base64, UUID}
import scala.util.{Failure, Success, Try}

package object utils {
  type ErrorOr[A] = Either[Throwable, A]

  final val expireIn: Long = 600000L

  final val formatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  def decodeBase64(encoded: String): String = {
    val decoded: Array[Byte] = Base64.getDecoder.decode(encoded)
    new String(decoded)
  }

  def toOffsetDateTime(str: String): OffsetDateTime = OffsetDateTime.parse(str, formatter)

  implicit val uuidFormat: JsonFormat[UUID] =
    new JsonFormat[UUID] {
      override def write(obj: UUID): JsValue = JsString(obj.toString)

      override def read(json: JsValue): UUID = json match {
        case JsString(s) =>
          Try(UUID.fromString(s)) match {
            case Success(result) => result
            case Failure(exception) =>
              deserializationError(s"could not parse $s as UUID", exception)
          }
        case notAJsString =>
          deserializationError(s"expected a String but got a ${notAJsString.compactPrint}")
      }
    }

  implicit val offsetDateTimeFormat: JsonFormat[OffsetDateTime] =
    new JsonFormat[OffsetDateTime] {
      override def write(obj: OffsetDateTime): JsValue = JsString(obj.format(formatter))

      override def read(json: JsValue): OffsetDateTime = json match {
        case JsString(s) =>
          Try(toOffsetDateTime(s)) match {
            case Success(result) => result
            case Failure(exception) =>
              deserializationError(s"could not parse $s as java OffsetDateTime", exception)
          }
        case notAJsString =>
          deserializationError(s"expected a String but got a ${notAJsString.compactPrint}")
      }
    }

  implicit val uriFormat: JsonFormat[URI] =
    new JsonFormat[URI] {
      override def write(obj: URI): JsValue = JsString(obj.toString)

      override def read(json: JsValue): URI = json match {
        case JsString(s) =>
          Try(URI.create(s)) match {
            case Success(result) => result
            case Failure(exception) =>
              deserializationError(s"could not parse $s as URI", exception)
          }
        case notAJsString =>
          deserializationError(s"expected a String but got a ${notAJsString.compactPrint}")
      }
    }

}
