package it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route
import com.nimbusds.jose.JOSEException
import com.nimbusds.jwt.JWTClaimsSet
import com.typesafe.scalalogging.Logger
import it.pagopa.pdnd.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.pdnd.interop.uservice.agreementmanagement
import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.{model => AgreementManagementDependency}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.AuthApiService
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.utils.expireIn
import it.pagopa.pdnd.interop.uservice.authorizationprocess.error._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.client.model.{EService, EServiceDescriptor}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.client.{model => CatalogManagementDependency}
import it.pagopa.pdnd.interop.uservice.keymanagement.client.{model => AuthorizationManagementDependency}
import it.pagopa.pdnd.interop.commons.utils.TypeConversions.{OptionOps, StringOps, TryOps}
import it.pagopa.pdnd.interop.commons.utils.errors.MissingBearer
import org.slf4j.LoggerFactory

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

  val logger = Logger.takingImplicit[ContextFieldsToLog](LoggerFactory.getLogger(this.getClass))

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
    logger.info("Creating client assertion token for clientId {}", clientId)
    val token: Future[String] =
      for {
        m2mToken  <- m2mAuthorizationService.token
        validated <- jwtValidator.validate(clientAssertion, clientAssertionType, grantType, clientId)(m2mToken)
        (clientId, assertion) = validated
        clientUuid <- clientId.toFutureUUID
        client     <- authorizationManagementService.getClient(clientUuid)(m2mToken)
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

  private def manageError(error: Throwable)(implicit contexts: Seq[(String, String)]): Route = {
    logger.error("Error while executing the request", error)
    error match {
      case ex @ MissingBearer  => createToken401(problemOf(StatusCodes.Unauthorized, "0001", ex))
      case ex: ParseException  => createToken401(problemOf(StatusCodes.Unauthorized, "0002", ex))
      case ex: JOSEException   => createToken401(problemOf(StatusCodes.Unauthorized, "0003", ex))
      case ex @ InvalidJWTSign => createToken401(problemOf(StatusCodes.Unauthorized, "0004", ex))
      case ex: InvalidAccessTokenRequest =>
        createToken400(problemOf(StatusCodes.BadRequest, "0005", ex, ex.errors.mkString(", ")))
      case ex =>
        createToken400(
          problemOf(StatusCodes.BadRequest, "0006", ex, "Something went wrong during access token request")
        )
    }
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
    logger.info("Validating bearer token")
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
