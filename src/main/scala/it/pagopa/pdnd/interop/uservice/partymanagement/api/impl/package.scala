package it.pagopa.pdnd.interop.uservice.partymanagement.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.{
  AccessTokenRequest,
  ClientCredentialsResponse,
  Problem
}
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.uuidFormat
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val problemFormat: RootJsonFormat[Problem] = jsonFormat3(Problem)

  implicit val accessTokenRequestFormat: RootJsonFormat[AccessTokenRequest] = jsonFormat4(AccessTokenRequest)
  implicit val clientCredentialsResponseFormat: RootJsonFormat[ClientCredentialsResponse] = jsonFormat3(
    ClientCredentialsResponse
  )

}
