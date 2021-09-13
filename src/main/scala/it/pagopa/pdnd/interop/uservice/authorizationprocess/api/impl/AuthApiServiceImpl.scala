package it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route
import cats.implicits.toTraverseOps
import com.nimbusds.jose.JOSEException
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.AuthApiService
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.utils.{EitherOps, expireIn, toUuid}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.error.{
  EnumParameterError,
  UnauthenticatedError,
  UuidConversionError
}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service._
import it.pagopa.pdnd.interop.uservice.keymanagement.client.invoker.{ApiError => AuthorizationManagementApiError}
import it.pagopa.pdnd.interop.uservice.{keymanagement, partymanagement}
import it.pagopa.pdnd.interopuservice.catalogprocess
import it.pagopa.pdnd.interopuservice.catalogprocess.client.invoker.{ApiError => CatalogProcessApiError}

import java.text.ParseException
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@SuppressWarnings(Array("org.wartremover.warts.Any"))
class AuthApiServiceImpl(
  jwtValidator: JWTValidator,
  jwtGenerator: JWTGenerator,
  agreementProcessService: AgreementProcessService,
  authorizationManagementService: AuthorizationManagementService,
  catalogProcessService: CatalogProcessService,
  partyManagementService: PartyManagementService
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
        bearerToken  <- extractBearer(contexts)
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
    case ex @ UnauthenticatedError => createToken401(Problem(Option(ex.getMessage), 401, "Not authorized"))
    case ex: ParseException        => createToken401(Problem(Option(ex.getMessage), 401, "Not authorized"))
    case ex: JOSEException         => createToken401(Problem(Option(ex.getMessage), 401, "Not authorized"))
    case ex                        => createToken400(Problem(Option(ex.getMessage), 400, "Something goes wrong during access token request"))
  }

  /** Code: 200, Message: Client created, DataType: Client
    * Code: 404, Message: Not Found, DataType: Problem
    * Code: 500, Message: Internal Server Error, DataType: Problem
    */
  override def createClient(clientSeed: ClientSeed)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route = {
    val result = for {
      bearerToken <- extractBearer(contexts)
      _           <- catalogProcessService.getEService(bearerToken, clientSeed.eServiceId.toString)
      client <- authorizationManagementService.createClient(
        clientSeed.eServiceId,
        clientSeed.name,
        clientSeed.description
      )
    } yield AuthorizationManagementService.clientToApi(client)

    onComplete(result) {
      case Success(client)                    => createClient201(client)
      case Failure(ex @ UnauthenticatedError) => createClient401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: CatalogProcessApiError[_]) if ex.code == 404 =>
        createClient404(Problem(Some(s"E-Service id ${clientSeed.eServiceId.toString} not found"), 404, "Not found"))
      case Failure(ex) => createClient500(Problem(Option(ex.getMessage), 500, "Error on client creation"))
    }
  }

  /** Code: 200, Message: Client retrieved, DataType: Client
    * Code: 404, Message: Client not found, DataType: Problem
    * Code: 500, Message: Internal server error, DataType: Problem
    */
  override def getClient(clientId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClient: ToEntityMarshaller[ClientDetail],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      bearerToken  <- extractBearer(contexts)
      client       <- authorizationManagementService.getClient(clientId)
      clientDetail <- getClientDetail(bearerToken, client)
    } yield clientDetail

    onComplete(result) {
      case Success(client)                    => getClient200(client)
      case Failure(ex @ UnauthenticatedError) => getClient401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        getClient404(Problem(Some(ex.message), 404, "Client not found"))
      case Failure(ex) => getClient500(Problem(Option(ex.getMessage), 500, "Error on client retrieve"))
    }
  }

  /** Code: 200, Message: Request succeed, DataType: Seq[Client]
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 500, Message: Internal Server Error, DataType: Problem
    */
  override def listClients(
    offset: Option[Int],
    limit: Option[Int],
    eServiceId: Option[String],
    operatorId: Option[String]
  )(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientDetailarray: ToEntityMarshaller[Seq[ClientDetail]],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      bearerToken  <- extractBearer(contexts)
      eServiceUuid <- eServiceId.map(toUuid).sequence.toFuture
      operatorUuid <- operatorId.map(toUuid).sequence.toFuture
      clients      <- authorizationManagementService.listClients(offset, limit, eServiceUuid, operatorUuid)
      // TODO Improve multiple requests
      clientsDetails <- clients.traverse(client => getClientDetail(bearerToken, client))
    } yield clientsDetails

    onComplete(result) {
      case Success(clients)                   => listClients200(clients)
      case Failure(ex: UuidConversionError)   => listClients400(Problem(Option(ex.getMessage), 400, "Bad request"))
      case Failure(ex @ UnauthenticatedError) => listClients401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex)                        => listClients500(Problem(Option(ex.getMessage), 500, "Error on clients list"))
    }
  }

  /** Code: 204, Message: Client deleted
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client not found, DataType: Problem
    * Code: 500, Message: Internal server error, DataType: Problem
    */
  override def deleteClient(
    clientId: String
  )(implicit contexts: Seq[(String, String)], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route = {
    val result = for {
      _ <- extractBearer(contexts)
      _ <- authorizationManagementService.deleteClient(clientId)
    } yield ()

    onComplete(result) {
      case Success(_)                         => deleteClient204
      case Failure(ex @ UnauthenticatedError) => deleteClient401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        deleteClient404(Problem(Option(ex.getMessage), 404, "Client not found"))
      case Failure(ex) => deleteClient500(Problem(Option(ex.getMessage), 500, "Error on client deletion"))
    }
  }

  /** Code: 201, Message: Operator added, DataType: Client
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Missing Required Information, DataType: Problem
    * Code: 500, Message: Internal server error, DataType: Problem
    */
  override def addOperator(clientId: String, operatorSeed: OperatorSeed)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route = {
    val result = for {
      _          <- extractBearer(contexts)
      clientUuid <- toUuid(clientId).toFuture
      client     <- authorizationManagementService.addOperator(clientUuid, operatorSeed.operatorId)
    } yield AuthorizationManagementService.clientToApi(client)

    onComplete(result) {
      case Success(client)                    => addOperator201(client)
      case Failure(ex @ UnauthenticatedError) => addOperator401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: UuidConversionError)   => addOperator400(Problem(Option(ex.getMessage), 400, "Bad request"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        addOperator404(Problem(Some(ex.message), 404, "Client not found"))
      case Failure(ex) => addOperator500(Problem(Option(ex.getMessage), 500, "Error on operator addition"))
    }
  }

  /** Code: 204, Message: Operator removed
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client or operator not found, DataType: Problem
    * Code: 500, Message: Internal server error, DataType: Problem
    */
  override def removeClientOperator(clientId: String, operatorId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _            <- extractBearer(contexts)
      clientUuid   <- toUuid(clientId).toFuture
      operatorUuid <- toUuid(operatorId).toFuture
      _            <- authorizationManagementService.removeClientOperator(clientUuid, operatorUuid)
    } yield ()

    onComplete(result) {
      case Success(_) => removeClientOperator204
      case Failure(ex @ UnauthenticatedError) =>
        removeClientOperator401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: UuidConversionError) =>
        removeClientOperator400(Problem(Option(ex.getMessage), 400, "Bad request"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        removeClientOperator404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(ex) => removeClientOperator500(Problem(Option(ex.getMessage), 500, "Error on operator removal"))
    }
  }

  /** Code: 200, Message: returns the corresponding key, DataType: Key
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Key not found, DataType: Problem
    * Code: 500, Message: Internal Server Error, DataType: Problem
    */
  override def getClientKeyById(clientId: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerKey: ToEntityMarshaller[Key],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _          <- extractBearer(contexts)
      clientUuid <- toUuid(clientId).toFuture
      key        <- authorizationManagementService.getKey(clientUuid, keyId)
    } yield AuthorizationManagementService.keyToApi(key)

    onComplete(result) {
      case Success(key) => getClientKeyById200(key)
      case Failure(ex @ UnauthenticatedError) =>
        getClientKeyById401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        getClientKeyById404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(ex) => getClientKeyById500(Problem(Option(ex.getMessage), 500, "Error on key retrieve"))
    }
  }

  /** Code: 204, Message: the corresponding key has been deleted.
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Key not found, DataType: Problem
    * Code: 500, Message: Internal Server Error, DataType: Problem
    */
  override def deleteClientKeyById(clientId: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _          <- extractBearer(contexts)
      clientUuid <- toUuid(clientId).toFuture
      _          <- authorizationManagementService.deleteKey(clientUuid, keyId)
    } yield ()

    onComplete(result) {
      case Success(_) => deleteClientKeyById204
      case Failure(ex @ UnauthenticatedError) =>
        deleteClientKeyById401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        deleteClientKeyById404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(ex) => deleteClientKeyById500(Problem(Option(ex.getMessage), 500, "Error on key delete"))
    }
  }

  /** Code: 204, Message: the corresponding key has been enabled.
    * Code: 404, Message: Key not found, DataType: Problem
    */
  override def enableKeyById(clientId: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _          <- extractBearer(contexts)
      clientUuid <- toUuid(clientId).toFuture
      _          <- authorizationManagementService.enableKey(clientUuid, keyId)
    } yield ()

    onComplete(result) {
      case Success(_)                         => enableKeyById204
      case Failure(ex @ UnauthenticatedError) => enableKeyById401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        enableKeyById404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(ex) => enableKeyById500(Problem(Option(ex.getMessage), 500, "Error on key enabling"))
    }
  }

  /** Code: 204, Message: the corresponding key has been disabled.
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Key not found, DataType: Problem
    * Code: 500, Message: Internal Server Error, DataType: Problem
    */
  override def disableKeyById(clientId: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _          <- extractBearer(contexts)
      clientUuid <- toUuid(clientId).toFuture
      _          <- authorizationManagementService.disableKey(clientUuid, keyId)
    } yield ()

    onComplete(result) {
      case Success(_) => disableKeyById204
      case Failure(ex @ UnauthenticatedError) =>
        disableKeyById401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        disableKeyById404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(ex) => disableKeyById500(Problem(Option(ex.getMessage), 500, "Error on key disabling"))
    }
  }

  /** Code: 201, Message: Keys created, DataType: Keys
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client id not found, DataType: Problem
    * Code: 500, Message: Internal Server Error, DataType: Problem
    */
  override def createKeys(clientId: String, keysSeeds: Seq[KeySeed])(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerKeys: ToEntityMarshaller[Keys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _            <- extractBearer(contexts)
      clientUuid   <- toUuid(clientId).toFuture
      seeds        <- keysSeeds.map(AuthorizationManagementService.toClientKeySeed).sequence.toFuture
      keysResponse <- authorizationManagementService.createKeys(clientUuid, seeds)
    } yield Keys(keysResponse.keys.map(AuthorizationManagementService.keyToApi))

    onComplete(result) {
      case Success(keys)                      => createKeys201(keys)
      case Failure(ex @ UnauthenticatedError) => createKeys401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: EnumParameterError)    => createKeys400(Problem(Option(ex.getMessage), 400, "Bad Request"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        createKeys404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(ex) => createKeys500(Problem(Option(ex.getMessage), 500, "Error on key creation"))
    }
  }

  /** Code: 200, Message: returns the corresponding array of keys, DataType: Keys
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client id not found, DataType: Problem
    * Code: 500, Message: Internal Server Error, DataType: Problem
    */
  override def getClientKeys(clientId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerKeys: ToEntityMarshaller[Keys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _            <- extractBearer(contexts)
      clientUuid   <- toUuid(clientId).toFuture
      keysResponse <- authorizationManagementService.getClientKeys(clientUuid)
    } yield Keys(keysResponse.keys.map(AuthorizationManagementService.keyToApi))

    onComplete(result) {
      case Success(keys)                      => getClientKeys200(keys)
      case Failure(ex @ UnauthenticatedError) => getClientKeys401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        getClientKeys404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(ex) => getClientKeys500(Problem(Option(ex.getMessage), 500, "Error on client keys retrieve"))
    }
  }

  private[this] def getClientDetail(
    bearerToken: String,
    client: keymanagement.client.model.Client
  ): Future[ClientDetail] = {
    for {
      eService     <- catalogProcessService.getEService(bearerToken, client.eServiceId.toString)
      organization <- partyManagementService.getOrganization(eService.producerId)
    } yield clientDetailToApi(client, eService, organization)
  }

  private[this] def clientDetailToApi(
    client: keymanagement.client.model.Client,
    eService: catalogprocess.client.model.EService,
    organization: partymanagement.client.model.Organization
  ): ClientDetail =
    ClientDetail(
      id = client.id,
      eService = CatalogProcessService.eServiceToApi(eService),
      organization = PartyManagementService.organizationToApi(organization),
      name = client.name,
      description = client.description
    )

}
