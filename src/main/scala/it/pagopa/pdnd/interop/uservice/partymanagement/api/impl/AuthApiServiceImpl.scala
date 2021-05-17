package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import com.auth0.jwt.exceptions.{
  AlgorithmMismatchException,
  InvalidClaimException,
  SignatureVerificationException,
  TokenExpiredException
}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.AuthApiService
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.{
  AccessTokenRequest,
  ClientCredentialsResponse,
  Problem
}
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.expireIn
import it.pagopa.pdnd.interop.uservice.partymanagement.model.token.TokenSeed
import it.pagopa.pdnd.interop.uservice.partymanagement.service.{JWTGenerator, JWTValidator}

import scala.util.Try

class AuthApiServiceImpl(jwtValidator: JWTValidator, jwtGenerator: JWTGenerator) extends AuthApiService {

  /** Code: 200, Message: an Access token, DataType: ClientCredentialsResponse
    * Code: 403, Message: Unauthorized, DataType: Problem
    * Code: 400, Message: Bad request, DataType: Problem
    */

  override def createToken(accessTokenRequest: AccessTokenRequest)(implicit
    toEntityMarshallerClientCredentialsResponse: ToEntityMarshaller[ClientCredentialsResponse],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {

    val token: Try[String] =
      for {
        validated <- jwtValidator.validate(accessTokenRequest.client_assertion)
        seed = TokenSeed.create(validated)
        token <- jwtGenerator.generate(seed)
      } yield token

    token.fold(ex => manageError(ex), tk => createToken200(ClientCredentialsResponse(tk, "tokenType", expireIn)))
  }

  private def manageError(error: Throwable): Route = error match {
    case ex: AlgorithmMismatchException =>
      createToken403(Problem(Option(ex.getMessage), 403, "Algorithm mismatch found"))
    case ex: SignatureVerificationException => createToken403(Problem(Option(ex.getMessage), 401, "Invalid signature"))
    case ex: TokenExpiredException          => createToken403(Problem(Option(ex.getMessage), 401, "Token expired"))
    case ex: InvalidClaimException          => createToken403(Problem(Option(ex.getMessage), 401, "Invalid claim found"))
    case ex                                 => createToken400(Problem(Option(ex.getMessage), 400, "Something goes wrong during access token request"))
  }

}
