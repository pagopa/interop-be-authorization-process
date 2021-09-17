package it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route
import com.nimbusds.jose.JOSEException
import it.pagopa.pdnd.interop.uservice.agreementmanagement
import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.model.AgreementEnums
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.AuthApiService
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.utils.expireIn
import it.pagopa.pdnd.interop.uservice.authorizationprocess.error._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service._

import java.text.ParseException
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@SuppressWarnings(Array("org.wartremover.warts.Any"))
class AuthApiServiceImpl(
  jwtValidator: JWTValidator,
  jwtGenerator: JWTGenerator,
  authorizationManagementService: AuthorizationManagementService,
  agreementManagementService: AgreementManagementService,
  catalogManagementService: CatalogManagementService,
  m2mAuthorizationService: M2MAuthorizationService
)(implicit ec: ExecutionContext)
    extends AuthApiService {

  /** Code: 200, Message: an Access token, DataType: ClientCredentialsResponse
    * Code: 403, Message: Unauthorized, DataType: Problem
    * Code: 400, Message: Bad request, DataType: Problem
    */

  override def createToken(accessTokenRequest: AccessTokenRequest)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientCredentialsResponse: ToEntityMarshaller[ClientCredentialsResponse],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {

    val token: Future[String] =
      for {
        m2mToken  <- m2mAuthorizationService.token
        validated <- jwtValidator.validate(accessTokenRequest)
        (clientId, assertion) = validated
        client <- authorizationManagementService.getClient(clientId)
        agreements <- agreementManagementService.getAgreements(
          m2mToken,
          client.consumerId.toString,
          client.eServiceId.toString,
          Some(AgreementEnums.Status.Active)
        )
        _        <- validateActiveAgreement(agreements, client.eServiceId.toString, client.consumerId.toString)
        eservice <- catalogManagementService.getEService(m2mToken, client.eServiceId.toString)
        token    <- jwtGenerator.generate(assertion, eservice.audience.toList)
      } yield token

    onComplete(token) {
      case Success(tk) => createToken200(ClientCredentialsResponse(tk, "tokenType", expireIn))
      case Failure(ex) => manageError(ex)
    }
  }

  private def validateActiveAgreement(
    agreements: Seq[agreementmanagement.client.model.Agreement],
    eserviceId: String,
    consumerId: String
  ): Future[Unit] = {
    val errorFunc: (String, String) => Throwable =
      if (agreements.isEmpty) AgreementNotFoundError
      else TooManyActiveAgreementsError

    Future.fromTry {
      Either
        .cond(agreements.size == 1, (), errorFunc(eserviceId, consumerId))
        .toTry
    }
  }

  private def manageError(error: Throwable): Route = error match {
    case ex @ UnauthenticatedError => createToken401(Problem(Option(ex.getMessage), 401, "Not authorized"))
    case ex: ParseException        => createToken401(Problem(Option(ex.getMessage), 401, "Not authorized"))
    case ex: JOSEException         => createToken401(Problem(Option(ex.getMessage), 401, "Not authorized"))
    case ex                        => createToken400(Problem(Option(ex.getMessage), 400, "Something goes wrong during access token request"))
  }
}
