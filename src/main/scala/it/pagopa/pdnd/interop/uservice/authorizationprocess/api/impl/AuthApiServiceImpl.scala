package it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import cats.implicits._
import com.auth0.jwt.exceptions.{
  AlgorithmMismatchException,
  InvalidClaimException,
  SignatureVerificationException,
  TokenExpiredException
}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.AuthApiService
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.utils.{decodeBase64, expireIn}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.token.TokenSeed
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.{
  AccessTokenRequest,
  ClientCredentialsResponse,
  Problem
}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{JWTGenerator, JWTValidator, VaultService}

import scala.util.{Failure, Success, Try}

class AuthApiServiceImpl(vaultService: VaultService, jwtValidator: JWTValidator, jwtGenerator: JWTGenerator)
    extends AuthApiService {

  /** Code: 200, Message: Success, DataType: Seq[String]
    * Code: 404, Message: Not found, DataType: Problem
    */
  override def getClientCredentials(
    clientId: String
  )(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route = {
    val paths = vaultService
      .getKeysList(s"secret/data/pdnd-interop-dev/keys/organizations/$clientId")

    val keys = paths.traverse(path =>
      vaultService
        .getSecret(path)
        .get("public")
        .map(decodeBase64)
        .toRight(new RuntimeException(s"Public key not found for $path"))
        .toTry
    )

    keys match {
      case Success(ks) => getClientCredentials200(ks)
      case Failure(ex) => getClientCredentials404(Problem(Option(ex.getMessage), 404, "some error"))
    }

  }

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
      createToken401(Problem(Option(ex.getMessage), 401, "Algorithm mismatch found"))
    case ex: SignatureVerificationException => createToken401(Problem(Option(ex.getMessage), 401, "Invalid signature"))
    case ex: TokenExpiredException          => createToken401(Problem(Option(ex.getMessage), 401, "Token expired"))
    case ex: InvalidClaimException          => createToken401(Problem(Option(ex.getMessage), 401, "Invalid claim found"))
    case ex                                 => createToken400(Problem(Option(ex.getMessage), 400, "Something goes wrong during access token request"))
  }

}
