package it.pagopa.pdnd.interop.uservice.authorizationprocess.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive1, Route}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.http.scaladsl.unmarshalling.FromStringUnmarshaller
import it.pagopa.pdnd.interop.uservice.authorizationprocess.server.AkkaHttpHelper._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.ClientCredentialsResponse
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.Problem
import java.util.UUID
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.ValidJWT

class AuthApi(
  authService: AuthApiService,
  authMarshaller: AuthApiMarshaller,
  wrappingDirective: Directive1[Seq[(String, String)]]
) {

  import authMarshaller._

  lazy val route: Route =
    path("as" / "token.oauth2") {
      post {
        wrappingDirective { implicit contexts =>
          formFields(
            "client_id".as[UUID].?,
            "client_assertion".as[String],
            "client_assertion_type".as[String],
            "grant_type".as[String]
          ) { (clientId, clientAssertion, clientAssertionType, grantType) =>
            authService.createToken(
              clientAssertion = clientAssertion,
              clientAssertionType = clientAssertionType,
              grantType = grantType,
              clientId = clientId
            )
          }

        }
      }
    } ~
      path("as" / "token" / "validate") {
        post {
          wrappingDirective { implicit contexts =>
            authService.validateToken()

          }
        }
      }
}

trait AuthApiService {
  def createToken200(responseClientCredentialsResponse: ClientCredentialsResponse)(implicit
    toEntityMarshallerClientCredentialsResponse: ToEntityMarshaller[ClientCredentialsResponse]
  ): Route =
    complete((200, responseClientCredentialsResponse))
  def createToken401(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    complete((401, responseProblem))
  def createToken400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    complete((400, responseProblem))

  /** Code: 200, Message: an Access token, DataType: ClientCredentialsResponse
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 400, Message: Bad request, DataType: Problem
    */
  def createToken(clientAssertion: String, clientAssertionType: String, grantType: String, clientId: Option[UUID])(
    implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientCredentialsResponse: ToEntityMarshaller[ClientCredentialsResponse],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route

  def validateToken200(responseValidJWT: ValidJWT)(implicit
    toEntityMarshallerValidJWT: ToEntityMarshaller[ValidJWT]
  ): Route =
    complete((200, responseValidJWT))
  def validateToken401(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((401, responseProblem))
  def validateToken400(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((400, responseProblem))

  /** Code: 200, Message: Client created, DataType: ValidJWT
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 400, Message: Bad request, DataType: Problem
    */
  def validateToken()(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerValidJWT: ToEntityMarshaller[ValidJWT],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route

}

trait AuthApiMarshaller {
  implicit def fromEntityUnmarshallerUUID: FromEntityUnmarshaller[UUID]

  implicit def toEntityMarshallerValidJWT: ToEntityMarshaller[ValidJWT]

  implicit def toEntityMarshallerClientCredentialsResponse: ToEntityMarshaller[ClientCredentialsResponse]

  implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem]

}
