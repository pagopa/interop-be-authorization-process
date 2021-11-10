package it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route
import com.nimbusds.jose.JOSEException
import com.nimbusds.jwt.JWTClaimsSet
import it.pagopa.pdnd.interop.uservice.agreementmanagement
import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.{model => AgreementManagementDependency}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.AuthApiService
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.system.TryOps
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.utils.{EitherOps, OptionOps, expireIn, toUuid}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.error._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.client.model.{EService, EServiceDescriptor}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.client.{model => CatalogManagementDependency}
import it.pagopa.pdnd.interop.uservice.keymanagement.client.{model => AuthorizationManagementDependency}

import java.text.ParseException
import java.time.ZoneOffset
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.ListHasAsScala
import scala.util.{Failure, Success, Try}

final case class AuthApiServiceImpl(
  jwtValidator: JWTValidator,
  jwtGenerator: JWTGenerator,
  authorizationManagementService: AuthorizationManagementService,
  agreementManagementService: AgreementManagementService,
  catalogManagementService: CatalogManagementService,
  m2mAuthorizationService: M2MAuthorizationService
)(implicit ec: ExecutionContext)
    extends AuthApiService {

  /** Code: 200, Message: an Access token, DataType: ClientCredentialsResponse
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 400, Message: Bad request, DataType: Problem
    */
  override def createToken(
    clientAssertion: String,
    clientAssertionType: String,
    grantType: String,
    clientId: Option[UUID]
  )(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientCredentialsResponse: ToEntityMarshaller[ClientCredentialsResponse],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {

    val token: Future[String] =
      for {
        m2mToken  <- m2mAuthorizationService.token
        validated <- jwtValidator.validate(clientAssertion, clientAssertionType, grantType, clientId)
        (clientId, assertion) = validated
        clientUuid <- toUuid(clientId).toFuture
        client     <- authorizationManagementService.getClient(clientUuid)
        _          <- clientMustBeActive(client)
        agreements <- agreementManagementService.getAgreements(
          m2mToken,
          client.consumerId,
          client.eServiceId,
          Some(AgreementManagementDependency.AgreementState.ACTIVE)
        )
        activeAgreement <- getActiveAgreement(agreements, client.eServiceId, client.consumerId)
        eservice        <- catalogManagementService.getEService(m2mToken, client.eServiceId)
        descriptor      <- getDescriptor(eservice, activeAgreement.descriptorId)
        descriptorAudience = descriptor.audience.toList
        _     <- descriptorMustBeActive(descriptor)
        token <- jwtGenerator.generate(assertion, descriptorAudience, client.purposes)
      } yield token

    onComplete(token) {
      case Success(tk) => createToken200(ClientCredentialsResponse(tk, TokenType.BEARER, expireIn))
      case Failure(ex) => manageError(ex)
    }
  }

  private def getActiveAgreement(
    agreements: Seq[agreementmanagement.client.model.Agreement],
    eserviceId: UUID,
    consumerId: UUID
  ): Future[agreementmanagement.client.model.Agreement] = {
    agreements match {
      case agreement :: Nil => Future.successful(agreement)
      case Nil              => Future.failed(AgreementNotFoundError(eserviceId, consumerId))
      case _                => Future.failed(TooManyActiveAgreementsError(eserviceId, consumerId))
    }
  }

  private def clientMustBeActive(client: ManagementClient): Future[Unit] =
    client.state match {
      case AuthorizationManagementDependency.ClientState.ACTIVE => Future.successful(())
      case _                                                    => Future.failed(ClientNotActive(client.id))
    }

  private def descriptorMustBeActive(descriptor: EServiceDescriptor): Future[Unit] =
    descriptor.state match {
      case CatalogManagementDependency.EServiceDescriptorState.DEPRECATED => Future.successful(())
      case CatalogManagementDependency.EServiceDescriptorState.PUBLISHED  => Future.successful(())
      case _                                                              => Future.failed(EServiceDescriptorNotActive(descriptor.id))
    }

  private def getDescriptor(eService: EService, descriptorId: UUID): Future[EServiceDescriptor] =
    eService.descriptors
      .find(_.id == descriptorId)
      .toFuture(DescriptorNotFound(eService.id, descriptorId))

  private def manageError(error: Throwable): Route = error match {
    case ex @ UnauthenticatedError     => createToken401(Problem(Option(ex.getMessage), 401, "Not authorized"))
    case ex: ParseException            => createToken401(Problem(Option(ex.getMessage), 401, "Not authorized"))
    case ex: JOSEException             => createToken401(Problem(Option(ex.getMessage), 401, "Not authorized"))
    case ex: InvalidAccessTokenRequest => createToken400(Problem(Option(ex.errors.mkString(", ")), 400, ex.getMessage))
    case ex                            => createToken400(Problem(Option(ex.getMessage), 400, "Something went wrong during access token request"))
  }

  def toResponseJWT(claims: JWTClaimsSet): Future[ValidJWT] = {
    Try {
      ValidJWT(
        iss = claims.getIssuer,
        sub = claims.getSubject,
        aud = claims.getAudience.asScala.toSeq,
        exp = claims.getExpirationTime.toInstant.atOffset(ZoneOffset.UTC),
        nbf = claims.getNotBeforeTime.toInstant.atOffset(ZoneOffset.UTC),
        iat = claims.getIssueTime.toInstant.atOffset(ZoneOffset.UTC),
        jti = claims.getJWTID
      )
    }.toFuture
  }

  /** Code: 200, Message: Client created, DataType: ValidJWT
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 400, Message: Bad request, DataType: Problem
    */
  override def validateToken()(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerValidJWT: ToEntityMarshaller[ValidJWT],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result: Future[ValidJWT] = for {
      bearer     <- tokenFromContext(contexts)
      claims     <- jwtValidator.validateBearer(bearer)
      validToken <- toResponseJWT(claims)
    } yield validToken

    onComplete(result) {
      case Success(tk) => validateToken200(tk)
      case Failure(ex) => manageError(ex)
    }
  }

  private[this] def tokenFromContext(context: Seq[(String, String)]): Future[String] =
    Future.fromTry(
      context
        .find(_._1 == "bearer")
        .map(header => header._2)
        .toRight(new RuntimeException("Bearer Token not provided"))
        .toTry
    )
}
