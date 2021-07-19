package it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route
import com.nimbusds.jose.JOSEException
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.AuthApiService
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.utils.expireIn
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.{
  AccessTokenRequest,
  ClientCredentialsResponse,
  Problem
}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{JWTGenerator, JWTValidator}

import java.text.ParseException
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class AuthApiServiceImpl(jwtValidator: JWTValidator, jwtGenerator: JWTGenerator)(implicit ec: ExecutionContext)
    extends AuthApiService {

  /** Code: 200, Message: an Access token, DataType: ClientCredentialsResponse
    * Code: 403, Message: Unauthorized, DataType: Problem
    * Code: 400, Message: Bad request, DataType: Problem
    */

  override def createToken(accessTokenRequest: AccessTokenRequest)(implicit
    toEntityMarshallerClientCredentialsResponse: ToEntityMarshaller[ClientCredentialsResponse],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {

    val token: Future[String] =
      for {
        validated <- jwtValidator.validate(accessTokenRequest)
        token     <- jwtGenerator.generate(validated)
      } yield token

    onComplete(token) {
      case Success(tk) => createToken200(ClientCredentialsResponse(tk, "tokenType", expireIn))
      case Failure(ex) => manageError(ex)
    }
  }

  private def manageError(error: Throwable): Route = error match {
    case ex: ParseException => createToken401(Problem(Option(ex.getMessage), 401, "Not authorized"))
    case ex: JOSEException  => createToken401(Problem(Option(ex.getMessage), 401, "Not authorized"))
    case ex                 => createToken400(Problem(Option(ex.getMessage), 400, "Something goes wrong during access token request"))
  }

}
