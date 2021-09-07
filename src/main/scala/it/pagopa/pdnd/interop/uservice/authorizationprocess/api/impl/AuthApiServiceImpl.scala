package it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route
import com.nimbusds.jose.JOSEException
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.AuthApiService
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.utils.expireIn
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service._
import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.invoker.{ApiError => AgreementManagementApiError}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.error.AgreementStatusNotValidError

import java.text.ParseException
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class AuthApiServiceImpl(
  jwtValidator: JWTValidator,
  jwtGenerator: JWTGenerator,
  agreementProcessService: AgreementProcessService,
  agreementManagementService: AgreementManagementService,
  authorizationManagementService: AuthorizationManagementService
)(implicit ec: ExecutionContext)
    extends AuthApiService
    with Validation {

  val bearerToken = "TODO" //TODO this is a fake token, evaluate how to deal with it in production

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
        validated    <- jwtValidator.validate(accessTokenRequest)
        pdndAudience <- agreementProcessService.retrieveAudience(bearerToken, accessTokenRequest.audience.toString)
        token        <- jwtGenerator.generate(validated, pdndAudience.audience.toList)
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

  /** Code: 200, Message: Client created, DataType: Client
    * Code: 404, Message: Not Found, DataType: Problem
    * Code: 500, Message: Internal Server Error, DataType: Problem
    */
  override def createClient(clientSeed: ClientSeed)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route = {
    val result = for {
      agreement <- agreementManagementService.retrieveAgreement(bearerToken, clientSeed.agreementId.toString)
      _         <- validateUsableAgreementStatus(agreement)
      client    <- authorizationManagementService.createClient(clientSeed.agreementId, clientSeed.description)
    } yield client

    onComplete(result) {
      case Success(client) => createClient201(client)
      case Failure(ex: AgreementStatusNotValidError) =>
        createClient422(Problem(Option(ex.getMessage), 422, "Invalid Agreement"))
      case Failure(ex: AgreementManagementApiError[_]) if ex.code == 404 =>
        createClient404(
          Problem(Some(s"Agreement id ${clientSeed.agreementId.toString} not found"), 404, "Agreement not found")
        )
      case Failure(ex) => createClient500(Problem(Option(ex.getMessage), 500, "Error on client creation"))
    }
  }
}
