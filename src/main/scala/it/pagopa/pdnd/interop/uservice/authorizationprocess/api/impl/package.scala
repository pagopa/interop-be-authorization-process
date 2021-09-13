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

  implicit val clientFormat: RootJsonFormat[Client]         = jsonFormat5(Client)
  implicit val clientSeedFormat: RootJsonFormat[ClientSeed] = jsonFormat3(ClientSeed)

  implicit val eServiceFormat: RootJsonFormat[EService]         = jsonFormat2(EService)
  implicit val organizationFormat: RootJsonFormat[Organization] = jsonFormat2(Organization)
  implicit val clientDetailFormat: RootJsonFormat[ClientDetail] = jsonFormat5(ClientDetail)

  implicit val operatorFormat: RootJsonFormat[Operator]         = jsonFormat6(Operator)
  implicit val operatorSeedFormat: RootJsonFormat[OperatorSeed] = jsonFormat1(OperatorSeed)

  implicit val primeInfoFormat: RootJsonFormat[OtherPrimeInfo] = jsonFormat3(OtherPrimeInfo)
  implicit val keyFormat: RootJsonFormat[Key]                  = jsonFormat22(Key)
  implicit val keysFormat: RootJsonFormat[Keys]                = jsonFormat1(Keys)
  implicit val keySeedFormat: RootJsonFormat[KeySeed]          = jsonFormat4(KeySeed)

  def extractBearer(contexts: Seq[(String, String)]): Future[String] = Future.fromTry {
    contexts.toMap.get("bearer").toRight(UnauthenticatedError).toTry
  }
}
