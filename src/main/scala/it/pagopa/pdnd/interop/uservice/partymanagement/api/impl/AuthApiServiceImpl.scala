package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import com.auth0.jwt.exceptions.{
  AlgorithmMismatchException,
  InvalidClaimException,
  SignatureVerificationException,
  TokenExpiredException
}
import com.auth0.jwt.interfaces.DecodedJWT
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.AuthApiService
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.{
  ClientCredentialsRequest,
  ClientCredentialsResponse,
  Problem
}
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.{ErrorOr, expireIn}
import it.pagopa.pdnd.interop.uservice.partymanagement.service.{JWTGenerator, JWTValidator}
import org.slf4j.{Logger, LoggerFactory}

class AuthApiServiceImpl(jwtValidator: JWTValidator, jwtGenerator: JWTGenerator) extends AuthApiService {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /** Code: 200, Message: an Access token, DataType: ClientCredentialsResponse
    * Code: 403, Message: Unauthorized, DataType: Problem
    * Code: 400, Message: Bad request, DataType: Problem
    */
  override def createToken(clientCredentialsRequest: ClientCredentialsRequest)(implicit
    toEntityMarshallerClientCredentialsResponse: ToEntityMarshaller[ClientCredentialsResponse],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    logger.debug(clientCredentialsRequest.toString)
    val validated: ErrorOr[DecodedJWT] = jwtValidator.validate(clientCredentialsRequest.client_assertion)
    val token: ErrorOr[String]         = validated.flatMap(jwt => jwtGenerator.generate(jwt.getAlgorithm))
    token.fold(ex => manageError(ex), tk => createToken200(ClientCredentialsResponse(tk, "tokenType", expireIn)))
  }

  private def manageError(error: Throwable): Route = error match {
    case ex: AlgorithmMismatchException =>
      createToken403(Problem(Option(ex.getMessage), 403, "Algorithm mismatch found"))
    case ex: SignatureVerificationException => createToken403(Problem(Option(ex.getMessage), 403, "Invalid signature"))
    case ex: TokenExpiredException          => createToken403(Problem(Option(ex.getMessage), 403, "Token expired"))
    case ex: InvalidClaimException          => createToken403(Problem(Option(ex.getMessage), 403, "Invalid claim found"))
    case ex                                 => createToken400(Problem(Option(ex.getMessage), 400, "Something goes wrong during access token request"))
  }

}
