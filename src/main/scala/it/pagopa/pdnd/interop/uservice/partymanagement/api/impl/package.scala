package it.pagopa.pdnd.interop.uservice.partymanagement.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.uuidFormat
import it.pagopa.pdnd.interop.uservice.securityprocess.model.{
  ClientCredentialsRequest,
  ClientCredentialsResponse,
  Problem
}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val problemFormat: RootJsonFormat[Problem] = jsonFormat3(Problem)

  implicit val clientCredentialsRequestFormat: RootJsonFormat[ClientCredentialsRequest] = jsonFormat4(
    ClientCredentialsRequest
  )
  implicit val clientCredentialsResponseFormat: RootJsonFormat[ClientCredentialsResponse] = jsonFormat3(
    ClientCredentialsResponse
  )

}
