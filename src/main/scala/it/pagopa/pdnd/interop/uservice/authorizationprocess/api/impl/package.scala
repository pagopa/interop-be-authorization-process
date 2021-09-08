package it.pagopa.pdnd.interop.uservice.authorizationprocess.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.utils.uuidFormat
import it.pagopa.pdnd.interop.uservice.authorizationprocess.error.UnauthenticatedError
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.Future

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val problemFormat: RootJsonFormat[Problem] = jsonFormat3(Problem)

  implicit val accessTokenRequestFormat: RootJsonFormat[AccessTokenRequest] = jsonFormat5(AccessTokenRequest)

  implicit val clientCredentialsResponseFormat: RootJsonFormat[ClientCredentialsResponse] = jsonFormat3(
    ClientCredentialsResponse
  )

  implicit val clientFormat: RootJsonFormat[Client]         = jsonFormat4(Client)
  implicit val clientSeedFormat: RootJsonFormat[ClientSeed] = jsonFormat2(ClientSeed)

  implicit val operatorSeedFormat: RootJsonFormat[OperatorSeed] = jsonFormat1(OperatorSeed)

  def extractBearer(contexts: Seq[(String, String)]): Future[String] = Future.fromTry {
    contexts.toMap.get("bearer").toRight(UnauthenticatedError).toTry
  }
}
