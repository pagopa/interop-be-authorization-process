package it.pagopa.pdnd.interop.uservice.authorizationprocess.server.impl

import akka.actor.CoordinatedShutdown
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.management.scaladsl.AkkaManagement
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import it.pagopa.interop.authorizationmanagement.client.api.{
  ClientApi => AuthorizationClientApi,
  KeyApi => AuthorizationKeyApi,
  PurposeApi => AuthorizationPurposeApi
}
import it.pagopa.pdnd.interop.commons.jwt.service.JWTReader
import it.pagopa.pdnd.interop.commons.jwt.service.impl.{DefaultJWTReader, getClaimsVerifier}
import it.pagopa.pdnd.interop.commons.jwt.{JWTConfiguration, KID, PublicKeysHolder, SerializedKey}
import it.pagopa.pdnd.interop.commons.utils.TypeConversions.TryOps
import it.pagopa.pdnd.interop.commons.utils.errors.GenericComponentErrors.ValidationRequestError
import it.pagopa.pdnd.interop.commons.utils.{CORSSupport, OpenapiUtils}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.{
  ClientApiMarshallerImpl,
  ClientApiServiceImpl,
  OperatorApiMarshallerImpl,
  OperatorApiServiceImpl,
  problemOf
}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.{ClientApi, OperatorApi}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.ApplicationConfiguration
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.system.{classicActorSystem, executionContext}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.server.Controller
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl.{
  AgreementManagementServiceImpl,
  AuthorizationManagementServiceImpl,
  CatalogManagementServiceImpl,
  PartyManagementServiceImpl,
  PurposeManagementServiceImpl,
  UserRegistryManagementServiceImpl
}
import it.pagopa.pdnd.interop.uservice.partymanagement.client.api.{PartyApi => PartyManagementApi}
import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.api.{AgreementApi => AgreementManagementApi}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.client.api.{EServiceApi => CatalogManagementApi}
import it.pagopa.pdnd.interop.uservice.purposemanagement.client.api.{PurposeApi => PurposeManagementApi}
import it.pagopa.pdnd.interop.uservice.userregistrymanagement.client.api.{UserApi => UserRegistryManagementApi}
import it.pagopa.pdnd.interop.uservice.userregistrymanagement.client.invoker.ApiKeyValue
import kamon.Kamon

import scala.concurrent.Future
import scala.util.{Failure, Success}
//shuts down the actor system in case of startup errors
case object StartupErrorShutdown extends CoordinatedShutdown.Reason

trait PartyManagementDependency {
  val partyManagementService: PartyManagementService = PartyManagementServiceImpl(
    PartyManagementInvoker(),
    PartyManagementApi(ApplicationConfiguration.getPartyManagementURL)
  )
}

trait UserRegistryManagementDependency {
  implicit val apiKey: ApiKeyValue = ApiKeyValue(ApplicationConfiguration.userRegistryApiKey)
  val userRegistryManagementService: UserRegistryManagementServiceImpl = UserRegistryManagementServiceImpl(
    UserRegistryManagementInvoker(),
    UserRegistryManagementApi(ApplicationConfiguration.getUserRegistryManagementURL)
  )
}

trait AuthorizationManagementDependency {
  val authorizationManagementClientApi: AuthorizationClientApi = AuthorizationClientApi(
    ApplicationConfiguration.getAuthorizationManagementURL
  )
  val authorizationManagementKeyApi: AuthorizationKeyApi = AuthorizationKeyApi(
    ApplicationConfiguration.getAuthorizationManagementURL
  )
  val authorizationManagementService: AuthorizationManagementService = AuthorizationManagementServiceImpl(
    AuthorizationManagementInvoker(),
    AuthorizationClientApi(ApplicationConfiguration.getAuthorizationManagementURL),
    AuthorizationKeyApi(ApplicationConfiguration.getAuthorizationManagementURL),
    AuthorizationPurposeApi(ApplicationConfiguration.getAuthorizationManagementURL)
  )
}

trait AgreementManagementDependency {
  val agreementManagementService: AgreementManagementService = AgreementManagementServiceImpl(
    AgreementManagementInvoker(),
    AgreementManagementApi(ApplicationConfiguration.getAgreementManagementURL)
  )
}

trait CatalogManagementDependency {
  val catalogManagementService: CatalogManagementService = CatalogManagementServiceImpl(
    CatalogManagementInvoker(),
    CatalogManagementApi(ApplicationConfiguration.getCatalogManagementURL)
  )
}

trait PurposeManagementDependency {
  val purposeManagementService: PurposeManagementService = PurposeManagementServiceImpl(
    PurposeManagementInvoker(),
    PurposeManagementApi(ApplicationConfiguration.getPurposeManagementURL)
  )
}

object Main
    extends App
    with CORSSupport
    with AgreementManagementDependency
    with CatalogManagementDependency
    with AuthorizationManagementDependency
    with PartyManagementDependency
    with PurposeManagementDependency
    with UserRegistryManagementDependency {

  val dependenciesLoaded: Future[JWTReader] = for {
    keyset <- JWTConfiguration.jwtReader.loadKeyset().toFuture
    jwtValidator = new DefaultJWTReader with PublicKeysHolder {
      var publicKeyset: Map[KID, SerializedKey] = keyset
      override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
        getClaimsVerifier(audience = ApplicationConfiguration.jwtAudience)
    }
  } yield jwtValidator

  dependenciesLoaded.transformWith {
    case Success(jwtValidator) => launchApp(jwtValidator)
    case Failure(ex) =>
      classicActorSystem.log.error(s"Startup error: ${ex.getMessage}")
      classicActorSystem.log.error(s"${ex.getStackTrace.mkString("\n")}")
      CoordinatedShutdown(classicActorSystem).run(StartupErrorShutdown)
  }

  private def launchApp(jwtReader: JWTReader): Future[Http.ServerBinding] = {
    Kamon.init()

    val clientApi: ClientApi = new ClientApi(
      ClientApiServiceImpl(
        authorizationManagementService,
        agreementManagementService,
        catalogManagementService,
        partyManagementService,
        purposeManagementService,
        userRegistryManagementService,
        jwtReader
      ),
      ClientApiMarshallerImpl,
      jwtReader.OAuth2JWTValidatorAsContexts
    )

    val operatorApi: OperatorApi = new OperatorApi(
      OperatorApiServiceImpl(authorizationManagementService, partyManagementService, jwtReader),
      OperatorApiMarshallerImpl,
      jwtReader.OAuth2JWTValidatorAsContexts
    )

    locally {
      val _ = AkkaManagement.get(classicActorSystem).start()
    }

    val controller: Controller = new Controller(
      clientApi,
      operatorApi,
      validationExceptionToRoute = Some(report => {
        val error =
          problemOf(
            StatusCodes.BadRequest,
            ValidationRequestError(OpenapiUtils.errorFromRequestValidationReport(report))
          )
        complete(error.status, error)(ClientApiMarshallerImpl.toEntityMarshallerProblem)
      })
    )

    val server: Future[Http.ServerBinding] =
      Http().newServerAt("0.0.0.0", ApplicationConfiguration.serverPort).bind(corsHandler(controller.routes))

    server
  }
}
