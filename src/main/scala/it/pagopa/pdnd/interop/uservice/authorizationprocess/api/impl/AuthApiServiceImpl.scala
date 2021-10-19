package it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route
import com.nimbusds.jose.JOSEException
import it.pagopa.pdnd.interop.uservice.agreementmanagement
import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.model.AgreementEnums
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.AuthApiService
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.utils.{OptionOps, expireIn}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.error._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.client.model.{
  EService,
  EServiceDescriptor,
  EServiceDescriptorEnums
}
import it.pagopa.pdnd.interop.uservice.keymanagement.client.model.ClientEnums

import java.text.ParseException
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@SuppressWarnings(Array("org.wartremover.warts.Any"))
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
        client <- authorizationManagementService.getClient(clientId)
        _      <- clientMustBeActive(client)
        agreements <- agreementManagementService.getAgreements(
          m2mToken,
          client.consumerId.toString,
          client.eServiceId.toString,
          Some(AgreementEnums.Status.Active)
        )
        activeAgreement <- getActiveAgreement(agreements, client.eServiceId.toString, client.consumerId.toString)
        eservice        <- catalogManagementService.getEService(m2mToken, client.eServiceId.toString)
        descriptor      <- getDescriptor(eservice, activeAgreement.descriptorId)
        descriptorAudience = descriptor.audience.toList
        _     <- descriptorMustBeActive(descriptor)
        token <- jwtGenerator.generate(assertion, descriptorAudience, client.purposes)
      } yield token

    onComplete(token) {
      case Success(tk) => createToken200(ClientCredentialsResponse(tk, "bearer", expireIn))
      case Failure(ex) => manageError(ex)
    }
  }

  private def getActiveAgreement(
    agreements: Seq[agreementmanagement.client.model.Agreement],
    eserviceId: String,
    consumerId: String
  ): Future[agreementmanagement.client.model.Agreement] = {
    agreements match {
      case agreement :: Nil => Future.successful(agreement)
      case Nil              => Future.failed(AgreementNotFoundError(eserviceId, consumerId))
      case _                => Future.failed(TooManyActiveAgreementsError(eserviceId, consumerId))
    }
  }

  private def clientMustBeActive(client: ManagementClient): Future[Unit] =
    client.status match {
      case ClientEnums.Status.Active => Future.successful(())
      case _                         => Future.failed(ClientNotActive(client.id.toString))
    }

  private def descriptorMustBeActive(descriptor: EServiceDescriptor): Future[Unit] =
    descriptor.status match {
      case EServiceDescriptorEnums.Status.Deprecated => Future.successful(())
      case EServiceDescriptorEnums.Status.Published  => Future.successful(())
      case _                                         => Future.failed(EServiceDescriptorNotActive(descriptor.id.toString))
    }

  private def getDescriptor(eService: EService, descriptorId: UUID): Future[EServiceDescriptor] =
    eService.descriptors
      .find(_.id == descriptorId)
      .toFuture(DescriptorNotFound(eService.id.toString, descriptorId.toString))

  private def manageError(error: Throwable): Route = error match {
    case ex @ UnauthenticatedError     => createToken401(Problem(Option(ex.getMessage), 401, "Not authorized"))
    case ex: ParseException            => createToken401(Problem(Option(ex.getMessage), 401, "Not authorized"))
    case ex: JOSEException             => createToken401(Problem(Option(ex.getMessage), 401, "Not authorized"))
    case ex: InvalidAccessTokenRequest => createToken400(Problem(Option(ex.errors.mkString(", ")), 400, ex.getMessage))
    case ex                            => createToken400(Problem(Option(ex.getMessage), 400, "Something went wrong during access token request"))
  }
}
