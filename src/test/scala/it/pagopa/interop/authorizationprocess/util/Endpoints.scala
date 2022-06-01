package it.pagopa.interop.authorizationprocess.util

import akka.http.scaladsl.client.RequestBuilding.{Delete, Get, Post, Put}
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import it.pagopa.interop.authorizationprocess.api.impl.ClientApiMarshallerImpl.sprayJsonUnmarshaller
import it.pagopa.interop.commons.utils.USER_ROLES
import spray.json.DefaultJsonProtocol._
import spray.json._

import java.util.UUID
import scala.util.Random

case class Endpoints(endpoints: Set[Endpoint]) {
  def endpointsMap: Map[String, Endpoint] = endpoints.map(e => e.route -> e).toMap
}

case class Endpoint(route: String, verb: String, roles: Seq[String]) {
  def contextsWithInvalidRole: Seq[(String, String)] = {
    Seq("bearer" -> "token", "uid" -> UUID.randomUUID().toString, USER_ROLES -> s"FakeRole-${Random.nextString(10)}")
  }

  def rolesInContexts: Seq[Seq[(String, String)]] = {
    roles.map(role => Seq("bearer" -> "token", "uid" -> UUID.randomUUID().toString, USER_ROLES -> role))
  }

  def asRequest: HttpRequest = verb match {
    case "GET"    => Get()
    case "POST"   => Post()
    case "DELETE" => Delete()
    case "PUT"    => Put()
  }
}

object AuthorizedRoutes {

  val lines = scala.io.Source.fromResource("authz.json").getLines().mkString

  implicit val endpointFormat: RootJsonFormat[Endpoint]   = jsonFormat3(Endpoint)
  implicit val endpointsFormat: RootJsonFormat[Endpoints] = jsonFormat1(Endpoints)

  implicit def fromEntityUnmarshallerClientSeed: FromEntityUnmarshaller[Endpoints] =
    sprayJsonUnmarshaller[Endpoints]

  val endpoints: Map[String, Endpoint] = lines.parseJson.convertTo[Endpoints].endpointsMap
}
