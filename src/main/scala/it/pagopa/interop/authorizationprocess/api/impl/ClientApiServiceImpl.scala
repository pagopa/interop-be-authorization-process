package it.pagopa.interop.authorizationprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import cats.implicits._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.authorizationmanagement
import it.pagopa.interop.authorizationmanagement.client.invoker.{ApiError => AuthorizationManagementApiError}
import it.pagopa.interop.authorizationmanagement.client.{model => AuthorizationmanagementDependency}
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors.{
  ClientCreationError,
  ClientDeletionError,
  ClientKeyCreationError,
  ClientKeyDeletionError,
  ClientKeyRetrievalError,
  ClientKeysRetrievalError,
  ClientListingError,
  ClientOperatorsRelationshipRetrievalError,
  ClientOperatorsRetrievalError,
  ClientRetrievalError,
  EncodedClientKeyRetrievalError,
  EnumParameterError,
  OperatorAdditionError,
  OperatorRelationshipAlreadyAssigned,
  OperatorRemovalError,
  SecurityOperatorRelationshipNotActive,
  SecurityOperatorRelationshipNotFound,
  UUIDConversionError
}
import it.pagopa.interop.authorizationprocess.service.{
  AgreementManagementService,
  AuthorizationManagementService,
  CatalogManagementService,
  PartyManagementService,
  PurposeManagementService,
  UserRegistryManagementService
}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.AkkaUtils.{getFutureBearer, getUidFuture}
import it.pagopa.interop.commons.utils.TypeConversions.{EitherOps, OptionOps, StringOps}
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.{MissingBearer, ResourceNotFoundError}
import it.pagopa.interop._
import it.pagopa.interop.agreementmanagement.client.{model => AgreementManagementDependency}
import it.pagopa.interop.authorizationprocess.api.ClientApiService
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors._
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationprocess.service.PartyManagementService.{
  relationshipProductToApi,
  relationshipRoleToApi,
  relationshipStateToApi
}
import it.pagopa.interop.catalogmanagement.client.{model => CatalogManagementDependency}
import it.pagopa.interop.partymanagement.client.model.{Problem => _, _}
import it.pagopa.interop.partymanagement.client.{model => PartyManagementDependency}
import it.pagopa.interop.purposemanagement.client.invoker.{ApiError => PurposeManagementApiError}
import it.pagopa.interop.purposemanagement.client.{model => PurposeManagementDependency}
import org.slf4j.LoggerFactory

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

final case class ClientApiServiceImpl(
  authorizationManagementService: AuthorizationManagementService,
  agreementManagementService: AgreementManagementService,
  catalogManagementService: CatalogManagementService,
  partyManagementService: PartyManagementService,
  purposeManagementService: PurposeManagementService,
  userRegistryManagementService: UserRegistryManagementService
)(implicit ec: ExecutionContext)
    extends ClientApiService {

  val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](LoggerFactory.getLogger(this.getClass))

  def internalServerError(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((500, responseProblem))

  /** Code: 201, Message: Client created, DataType: Client
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Not Found, DataType: Problem
    * Code: 500, Message: Internal Server Error, DataType: Problem
    */
  override def createConsumerClient(clientSeed: ClientSeed)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route = {
    logger.info("Creating CONSUMER client {} for and consumer {}", clientSeed.name, clientSeed.consumerId)
    val result = for {
      bearerToken <- getFutureBearer(contexts)
      client <- authorizationManagementService.createClient(
        clientSeed.consumerId,
        clientSeed.name,
        clientSeed.description,
        authorizationmanagement.client.model.ClientKind.CONSUMER
      )(bearerToken)
      apiClient <- getClient(bearerToken, client)
    } yield apiClient

    onComplete(result) {
      case Success(client) => createConsumerClient201(client)
      case Failure(MissingBearer) =>
        logger.error(
          "Error in creating client {} for consumer {}",
          clientSeed.name,
          clientSeed.consumerId,
          MissingBearer
        )
        createConsumerClient401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex) =>
        logger.error("Error in creating CONSUMER client {} for consumer {}", clientSeed.name, clientSeed.consumerId, ex)
        internalServerError(problemOf(StatusCodes.InternalServerError, ClientCreationError))
    }
  }

  override def createApiClient(clientSeed: ClientSeed)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route = {
    logger.info("Creating API client {} for and consumer {}", clientSeed.name, clientSeed.consumerId)
    val result = for {
      bearerToken <- getFutureBearer(contexts)
      client <- authorizationManagementService.createClient(
        clientSeed.consumerId,
        clientSeed.name,
        clientSeed.description,
        authorizationmanagement.client.model.ClientKind.API
      )(bearerToken)
      apiClient <- getClient(bearerToken, client)
    } yield apiClient

    onComplete(result) {
      case Success(client) => createApiClient201(client)
      case Failure(MissingBearer) =>
        logger.error(
          "Error in creating API client {} for consumer {}",
          clientSeed.name,
          clientSeed.consumerId,
          MissingBearer
        )
        createApiClient401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex) =>
        logger.error("Error in creating client {} for consumer {}", clientSeed.name, clientSeed.consumerId, ex)
        internalServerError(problemOf(StatusCodes.InternalServerError, ClientCreationError))
    }
  }

  /** Code: 200, Message: Client retrieved, DataType: Client
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client not found, DataType: Problem
    * Code: 500, Message: Internal server error, DataType: Problem
    */
  override def getClient(clientId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route = {
    logger.info("Getting client {}", clientId)
    val result = for {
      bearerToken <- getFutureBearer(contexts)
      clientUuid  <- clientId.toFutureUUID
      client      <- authorizationManagementService.getClient(clientUuid)(bearerToken)
      apiClient   <- getClient(bearerToken, client)
    } yield apiClient

    onComplete(result) {
      case Success(client) => getClient200(client)
      case Failure(MissingBearer) =>
        logger.error("Error in getting client {}", clientId, MissingBearer)
        getClient401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error("Error in getting client {}", clientId, ex)
        getClient404(problemOf(StatusCodes.NotFound, ResourceNotFoundError(s"Client id $clientId")))
      case Failure(ex) =>
        logger.error("Error in getting client {}", clientId, ex)
        internalServerError(problemOf(StatusCodes.InternalServerError, ClientRetrievalError))
    }
  }

  /** Code: 200, Message: Request succeed, DataType: Clients
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 401, Message: Unauthorized, DataType: Problem
    */
  override def listClients(
    consumerId: String,
    offset: Option[Int],
    limit: Option[Int],
    purposeId: Option[String],
    kind: Option[String]
  )(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClients: ToEntityMarshaller[Clients],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    logger.info("Listing clients (offset: {} and limit: {}) for operator {} for consumer {}", offset, limit, consumerId)

    // TODO Improve multiple requests
    val result: Future[Seq[Client]] = for {
      bearerToken  <- getFutureBearer(contexts)
      personId     <- getUidFuture(contexts).flatMap(_.toFutureUUID)
      consumerUuid <- consumerId.toFutureUUID
      clientKind   <- kind.traverse(AuthorizationmanagementDependency.ClientKind.fromValue).toFuture
      relationships <-
        partyManagementService
          .getRelationships(
            consumerUuid,
            personId,
            Seq(PartyManagementService.PRODUCT_ROLE_SECURITY_OPERATOR, PartyManagementService.PRODUCT_ROLE_ADMIN)
          )(bearerToken)
          .map(
            _.items
              .filter(_.state == RelationshipState.ACTIVE)
          )
      purposeUuid <- purposeId.traverse(_.toFutureUUID)
      managementClients <-
        if (relationships.exists(_.product.role == PartyManagementService.PRODUCT_ROLE_ADMIN))
          authorizationManagementService.listClients(
            offset = offset,
            limit = limit,
            relationshipId = None,
            consumerId = consumerUuid.some,
            purposeId = purposeUuid,
            kind = clientKind
          )(bearerToken)
        else
          relationships
            .map(_.id.some)
            .flatTraverse(relationshipId =>
              authorizationManagementService.listClients(
                offset = offset,
                limit = limit,
                relationshipId = relationshipId,
                consumerId = consumerUuid.some,
                purposeId = purposeUuid,
                kind = clientKind
              )(bearerToken)
            )

      clients <- managementClients.traverse(getClient(bearerToken, _))
    } yield clients

    onComplete(result) {
      case Success(clients) => listClients200(Clients(clients))
      case Failure(ex: UUIDConversionError) =>
        logger.error(
          "Error while listing clients (offset: {} and limit: {}) for operator {} for consumer {}",
          offset,
          limit,
          consumerId,
          ex
        )
        listClients400(problemOf(StatusCodes.BadRequest, ex))
      case Failure(MissingBearer) =>
        logger.error(
          "Error while listing clients (offset: {} and limit: {}) for operator {} for consumer {}",
          offset,
          limit,
          consumerId,
          MissingBearer
        )
        listClients401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex) =>
        logger.error(
          "Error while listing clients (offset: {} and limit: {}) for operator {} for consumer {}",
          offset,
          limit,
          consumerId,
          ex
        )
        internalServerError(problemOf(StatusCodes.InternalServerError, ClientListingError))
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
    logger.info("Deleting client {}", clientId)
    val result = for {
      bearerToken <- getFutureBearer(contexts)
      clientUuid  <- clientId.toFutureUUID
      _           <- authorizationManagementService.deleteClient(clientUuid)(bearerToken)
    } yield ()

    onComplete(result) {
      case Success(_) => deleteClient204
      case Failure(MissingBearer) =>
        logger.error("Error while deleting client {} - {}", clientId, MissingBearer)
        deleteClient401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error("Error while deleting client {} - {}", clientId, ex)
        deleteClient404(problemOf(StatusCodes.NotFound, ResourceNotFoundError(s"Client id $clientId")))
      case Failure(ex) =>
        logger.error("Error while deleting client {} - {}", clientId, ex)
        internalServerError(problemOf(StatusCodes.InternalServerError, ClientDeletionError))
    }
  }

  /** Code: 201, Message: Operator added, DataType: Client
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Missing Required Information, DataType: Problem
    * Code: 500, Message: Internal server error, DataType: Problem
    */
  override def clientOperatorRelationshipBinding(clientId: String, relationshipId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route = {
    logger.info("Binding client {} with relationship {}", clientId, relationshipId)
    val result = for {
      bearerToken      <- getFutureBearer(contexts)
      clientUUID       <- clientId.toFutureUUID
      relationshipUUID <- relationshipId.toFutureUUID
      client           <- authorizationManagementService.getClient(clientUUID)(bearerToken)
      relationship     <- getSecurityRelationship(relationshipUUID)(bearerToken)
      updatedClient <- client.relationships
        .find(_ === relationship.id)
        .fold(authorizationManagementService.addRelationship(clientUUID, relationship.id)(bearerToken))(_ =>
          Future.failed(OperatorRelationshipAlreadyAssigned(client.id, relationship.id))
        )
      apiClient <- getClient(bearerToken, updatedClient)
    } yield apiClient

    onComplete(result) {
      case Success(client) => clientOperatorRelationshipBinding201(client)
      case Failure(MissingBearer) =>
        logger.error("Error while binding client {} with relationship {}", clientId, relationshipId, MissingBearer)
        clientOperatorRelationshipBinding401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: UUIDConversionError) =>
        logger.error("Error while binding client {} with relationship {}", clientId, relationshipId, ex)
        clientOperatorRelationshipBinding400(problemOf(StatusCodes.BadRequest, ex))
      case Failure(ex: SecurityOperatorRelationshipNotActive) =>
        logger.error("Error while binding client {} with relationship {}", clientId, relationshipId, ex)
        clientOperatorRelationshipBinding400(problemOf(StatusCodes.BadRequest, ex))
      case Failure(ex: OperatorRelationshipAlreadyAssigned) =>
        logger.error("Error while binding client {} with relationship {}", clientId, relationshipId, ex)
        clientOperatorRelationshipBinding400(problemOf(StatusCodes.BadRequest, ex))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error("Error while binding client {} with relationship {}", clientId, relationshipId, ex)
        clientOperatorRelationshipBinding404(
          problemOf(
            StatusCodes.NotFound,
            ResourceNotFoundError(s"Client id $clientId - relationship id: $relationshipId")
          )
        )
      case Failure(ex) =>
        logger.error("Error while binding client {} with relationship {}", clientId, relationshipId, ex)
        internalServerError(problemOf(StatusCodes.InternalServerError, OperatorAdditionError))
    }
  }

  /** Code: 204, Message: Operator removed
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client or operator not found, DataType: Problem
    * Code: 500, Message: Internal server error, DataType: Problem
    */
  override def removeClientOperatorRelationship(clientId: String, relationshipId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    logger.info("Removing binding between client {} with relationship {}", clientId, relationshipId)
    val result = for {
      bearerToken      <- getFutureBearer(contexts)
      clientUUID       <- clientId.toFutureUUID
      relationshipUUID <- relationshipId.toFutureUUID
//      requesterRelationships <- partyManagementService.getRelationshipsByPersonId(userUUID,Seq.empty)
      _ <- authorizationManagementService.removeClientRelationship(clientUUID, relationshipUUID)(bearerToken)
    } yield ()

    onComplete(result) {
      case Success(_) => removeClientOperatorRelationship204
      case Failure(MissingBearer) =>
        logger.error("Removing binding between client {} with relationship {}", clientId, relationshipId, MissingBearer)
        removeClientOperatorRelationship401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: UUIDConversionError) =>
        logger.error("Removing binding between client {} with relationship {}", clientId, relationshipId, ex)
        removeClientOperatorRelationship400(problemOf(StatusCodes.BadRequest, ex))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error("Removing binding between client {} with relationship {}", clientId, relationshipId, ex)
        removeClientOperatorRelationship404(
          problemOf(
            StatusCodes.NotFound,
            ResourceNotFoundError(s"Client id $clientId - relationship id: $relationshipId")
          )
        )
      case Failure(ex) =>
        logger.error("Removing binding between client {} with relationship {}", clientId, relationshipId, ex)
        internalServerError(problemOf(StatusCodes.InternalServerError, OperatorRemovalError))
    }
  }

  /** Code: 200, Message: returns the corresponding key, DataType: Key
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Key not found, DataType: Problem
    * Code: 500, Message: Internal Server Error, DataType: Problem
    */
  override def getClientKeyById(clientId: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientKey: ToEntityMarshaller[ClientKey],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    logger.info("Getting client {} key by id {}", clientId, keyId)
    val result = for {
      bearerToken <- getFutureBearer(contexts)
      clientUuid  <- clientId.toFutureUUID
      key         <- authorizationManagementService.getKey(clientUuid, keyId)(bearerToken)
    } yield AuthorizationManagementService.keyToApi(key)

    onComplete(result) {
      case Success(key) => getClientKeyById200(key)
      case Failure(MissingBearer) =>
        logger.error("Error while getting client {} key by id {}", clientId, keyId, MissingBearer)
        getClientKeyById401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error("Error while getting client {} key by id {}", clientId, keyId, ex)
        getClientKeyById404(
          problemOf(StatusCodes.NotFound, ResourceNotFoundError(s"Client id $clientId - key id: $keyId"))
        )
      case Failure(ex) =>
        logger.error("Error while getting client {} key by id {}", clientId, keyId, ex)
        internalServerError(problemOf(StatusCodes.InternalServerError, ClientKeyRetrievalError))
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
    logger.info("Deleting client {} key by id {}", clientId, keyId)
    val result = for {
      bearerToken <- getFutureBearer(contexts)
      clientUuid  <- clientId.toFutureUUID
      _           <- authorizationManagementService.deleteKey(clientUuid, keyId)(bearerToken)
    } yield ()

    onComplete(result) {
      case Success(_) => deleteClientKeyById204
      case Failure(MissingBearer) =>
        logger.error("Error while deleting client {} key by id {}", clientId, keyId, MissingBearer)
        deleteClientKeyById401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error("Error while deleting client {} key by id {}", clientId, keyId, ex)
        deleteClientKeyById404(
          problemOf(StatusCodes.NotFound, ResourceNotFoundError(s"Client id $clientId - key id: $keyId"))
        )
      case Failure(ex) =>
        logger.error("Error while deleting client {} key by id {}", clientId, keyId, ex)
        internalServerError(problemOf(StatusCodes.InternalServerError, ClientKeyDeletionError))
    }
  }

  /** Code: 201, Message: Keys created, DataType: Keys
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client id not found, DataType: Problem
    * Code: 500, Message: Internal Server Error, DataType: Problem
    */
  override def createKeys(clientId: String, keysSeeds: Seq[KeySeed])(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    logger.info("Creating keys for client {}", clientId)
    val result = for {
      bearerToken <- getFutureBearer(contexts)
      clientUuid  <- clientId.toFutureUUID
      client      <- authorizationManagementService.getClient(clientUuid)(bearerToken)
      relationshipsIds <- keysSeeds.traverse(seed =>
        securityOperatorRelationship(client.consumerId, seed.operatorId)(bearerToken)
      )
      seeds <- keysSeeds
        .zip(relationshipsIds)
        .traverse {
          case (seed, Some(relationship)) =>
            Right(AuthorizationManagementService.toDependencyKeySeed(seed, relationship.id))
          case (seed, None) =>
            Left(SecurityOperatorRelationshipNotFound(client.consumerId, seed.operatorId))
        }
        .toFuture
      keysResponse <- authorizationManagementService.createKeys(clientUuid, seeds)(bearerToken)
    } yield ClientKeys(keysResponse.keys.map(AuthorizationManagementService.keyToApi))

    onComplete(result) {
      case Success(keys) => createKeys201(keys)
      case Failure(MissingBearer) =>
        logger.error("Error while creating keys for client {}", clientId, MissingBearer)
        createKeys401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: EnumParameterError) =>
        logger.error("Error while creating keys for client {}", clientId, ex)
        createKeys400(problemOf(StatusCodes.BadRequest, ex))
      case Failure(ex: SecurityOperatorRelationshipNotFound) =>
        logger.error("Error while creating keys for client {}", clientId, ex)
        createKeys403(problemOf(StatusCodes.Forbidden, ex))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error("Error while creating keys for client {}", clientId, ex)
        createKeys404(problemOf(StatusCodes.NotFound, ResourceNotFoundError(s"Client id $clientId")))
      case Failure(ex) =>
        logger.error("Error while creating keys for client {}", clientId, ex)
        internalServerError(problemOf(StatusCodes.InternalServerError, ClientKeyCreationError))
    }
  }

  /** Code: 200, Message: returns the corresponding array of keys, DataType: Keys
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client id not found, DataType: Problem
    * Code: 500, Message: Internal Server Error, DataType: Problem
    */
  override def getClientKeys(clientId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    logger.info("Getting keys of client {}", clientId)
    val result = for {
      bearerToken  <- getFutureBearer(contexts)
      clientUuid   <- clientId.toFutureUUID
      keysResponse <- authorizationManagementService.getClientKeys(clientUuid)(bearerToken)
    } yield ClientKeys(keysResponse.keys.map(AuthorizationManagementService.keyToApi))

    onComplete(result) {
      case Success(keys) => getClientKeys200(keys)
      case Failure(MissingBearer) =>
        logger.error("Error while getting keys of client {}", clientId, MissingBearer)
        getClientKeys401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error("Error while getting keys of client {}", clientId, ex)
        getClientKeys404(problemOf(StatusCodes.NotFound, ResourceNotFoundError(s"Client id $clientId ")))
      case Failure(ex) =>
        logger.error("Error while getting keys of client {}", clientId, ex)
        internalServerError(problemOf(StatusCodes.InternalServerError, ClientKeysRetrievalError))
    }
  }

  /** Code: 200, Message: Request succeed, DataType: Seq[Operator]
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Not Found, DataType: Problem
    */
  override def getClientOperators(clientId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerOperatorarray: ToEntityMarshaller[Seq[Operator]],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    logger.info("Getting operators of client {}", clientId)
    val result = for {
      bearerToken <- getFutureBearer(contexts)
      clientUuid  <- clientId.toFutureUUID
      client      <- authorizationManagementService.getClient(clientUuid)(bearerToken)
      operators   <- operatorsFromClient(client)(bearerToken)
    } yield operators

    onComplete(result) {
      case Success(keys) => getClientOperators200(keys)
      case Failure(MissingBearer) =>
        logger.error("Error while getting operators of client {}", clientId, MissingBearer)
        getClientOperators401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error("Error while getting operators of client {}", clientId, ex)
        getClientOperators404(problemOf(StatusCodes.NotFound, ResourceNotFoundError(s"Client id $clientId ")))
      case Failure(ex) =>
        logger.error("Error while getting operators of client {}", clientId, ex)
        internalServerError(problemOf(StatusCodes.InternalServerError, ClientOperatorsRetrievalError))
    }
  }

  /** Code: 200, Message: Client Operator retrieved, DataType: Operator
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client or Operator not found, DataType: Problem
    */
  override def getClientOperatorRelationshipById(clientId: String, relationshipId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerOperator: ToEntityMarshaller[Operator],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    logger.info("Getting operators of client {} by relationship {}", clientId, relationshipId)
    val result = for {
      bearerToken      <- getFutureBearer(contexts)
      clientUUID       <- clientId.toFutureUUID
      relationshipUUID <- relationshipId.toFutureUUID
      client           <- authorizationManagementService.getClient(clientUUID)(bearerToken)
      _                <- hasClientRelationship(client, relationshipUUID)
      operator         <- operatorFromRelationship(relationshipUUID)(bearerToken)
    } yield operator

    onComplete(result) {
      case Success(operator) => getClientOperatorRelationshipById200(operator)
      case Failure(MissingBearer) =>
        logger.error(
          "Error while getting operators of client {} by relationship {}",
          clientId,
          relationshipId,
          MissingBearer
        )
        getClientOperatorRelationshipById401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error("Error while getting operators of client {} by relationship {}", clientId, relationshipId, ex)
        getClientOperatorRelationshipById404(
          problemOf(
            StatusCodes.NotFound,
            ResourceNotFoundError(s"Client id $clientId - relationship id: $relationshipId")
          )
        )
      case Failure(ex: SecurityOperatorRelationshipNotFound) =>
        logger.error("Error while getting operators of client {} by relationship {}", clientId, relationshipId, ex)
        getClientOperatorRelationshipById404(problemOf(StatusCodes.NotFound, ex))
      case Failure(ex) =>
        logger.error("Error while getting operators of client {} by relationship {}", clientId, relationshipId, ex)
        internalServerError(problemOf(StatusCodes.InternalServerError, ClientOperatorsRelationshipRetrievalError))
    }
  }

  override def addClientPurpose(clientId: String, details: PurposeAdditionDetails)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    logger.info("Adding Purpose {} to Client {}", details.purposeId, clientId)

    def descriptorToComponentState(
      descriptor: CatalogManagementDependency.EServiceDescriptor
    ): AuthorizationmanagementDependency.ClientComponentState = descriptor.state match {
      case CatalogManagementDependency.EServiceDescriptorState.PUBLISHED =>
        AuthorizationmanagementDependency.ClientComponentState.ACTIVE
      case _ => AuthorizationmanagementDependency.ClientComponentState.INACTIVE
    }

    def agreementToComponentState(
      agreement: AgreementManagementDependency.Agreement
    ): AuthorizationmanagementDependency.ClientComponentState = agreement.state match {
      case AgreementManagementDependency.AgreementState.ACTIVE =>
        AuthorizationmanagementDependency.ClientComponentState.ACTIVE
      case _ => AuthorizationmanagementDependency.ClientComponentState.INACTIVE
    }

    def purposeToComponentState(
      purpose: PurposeManagementDependency.Purpose
    ): AuthorizationmanagementDependency.ClientComponentState =
      if (purpose.versions.exists(_.state == PurposeManagementDependency.PurposeVersionState.ACTIVE))
        AuthorizationmanagementDependency.ClientComponentState.ACTIVE
      else AuthorizationmanagementDependency.ClientComponentState.INACTIVE

    val result: Future[Unit] = for {
      bearerToken <- getFutureBearer(contexts)
      clientUuid  <- clientId.toFutureUUID
      purpose     <- purposeManagementService.getPurpose(bearerToken)(details.purposeId)
      eService    <- catalogManagementService.getEService(bearerToken)(purpose.eserviceId)
      agreements  <- agreementManagementService.getAgreements(bearerToken)(purpose.eserviceId, purpose.consumerId)
      agreement <- agreements
        .filter(_.state != AgreementManagementDependency.AgreementState.PENDING)
        .maxByOption(_.createdAt)
        .toFuture(ClientPurposeAddAgreementNotFound(purpose.eserviceId.toString, purpose.consumerId.toString))
      descriptor <- eService.descriptors
        .find(_.id == agreement.descriptorId)
        .toFuture(ClientPurposeAddDescriptorNotFound(purpose.eserviceId.toString, agreement.descriptorId.toString))
      states = AuthorizationmanagementDependency.ClientStatesChainSeed(
        eservice = AuthorizationmanagementDependency.ClientEServiceDetailsSeed(
          eserviceId = eService.id,
          state = descriptorToComponentState(descriptor),
          audience = descriptor.audience,
          voucherLifespan = descriptor.voucherLifespan
        ),
        agreement = AuthorizationmanagementDependency
          .ClientAgreementDetailsSeed(agreementId = agreement.id, state = agreementToComponentState(agreement)),
        purpose = AuthorizationmanagementDependency.ClientPurposeDetailsSeed(
          purposeId = purpose.id,
          state = purposeToComponentState(purpose)
        )
      )
      seed = AuthorizationmanagementDependency.PurposeSeed(details.purposeId, states)
      _ <- authorizationManagementService.addClientPurpose(clientUuid, seed)(bearerToken)
    } yield ()

    onComplete(result) {
      case Success(_) => addClientPurpose204
      case Failure(ex: PurposeManagementApiError[_]) if ex.code == 404 =>
        logger.error("Error adding purpose {} to client {}", details.purposeId, clientId, ex)
        createKeys404(problemOf(StatusCodes.NotFound, ResourceNotFoundError(s"Purpose id ${details.purposeId}")))
      case Failure(ex) =>
        logger.error("Error adding purpose {} to client {}", details.purposeId, clientId, ex)
        internalServerError(
          problemOf(StatusCodes.InternalServerError, ClientPurposeAddError(clientId, details.purposeId.toString))
        )
    }
  }

  override def removeClientPurpose(clientId: String, purposeId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    logger.info("Removing Purpose from Client {}", clientId)

    val result: Future[Unit] = for {
      bearerToken <- getFutureBearer(contexts)
      clientUuid  <- clientId.toFutureUUID
      purposeUuid <- purposeId.toFutureUUID
      _           <- authorizationManagementService.removeClientPurpose(clientUuid, purposeUuid)(bearerToken)
    } yield ()

    onComplete(result) {
      case Success(_) => addClientPurpose204
      case Failure(ex: PurposeManagementApiError[_]) if ex.code == 404 =>
        logger.error("Error removing purpose {} from client {}", clientId, ex)
        createKeys404(problemOf(StatusCodes.NotFound, ResourceNotFoundError(s"Purpose id $purposeId")))
      case Failure(ex) =>
        logger.error("Error removing purpose {} from client {}", clientId, ex)
        internalServerError(problemOf(StatusCodes.InternalServerError, ClientPurposeRemoveError(clientId, purposeId)))
    }
  }

  private[this] def getClient(bearerToken: String, client: AuthorizationmanagementDependency.Client): Future[Client] = {
    for {
      consumer  <- partyManagementService.getOrganization(client.consumerId)(bearerToken)
      operators <- operatorsFromClient(client)(bearerToken)
    } yield clientToApi(client, consumer, operators)
  }

  private[this] def getSecurityRelationship(
    relationshipId: UUID
  )(bearerToken: String): Future[partymanagement.client.model.Relationship] = {

    def isActiveSecurityOperatorRelationship(relationship: Relationship): Future[Boolean] = {
      val condition = relationship.product.role == PartyManagementService.PRODUCT_ROLE_SECURITY_OPERATOR &&
        relationship.role == PartyManagementDependency.PartyRole.OPERATOR &&
        relationship.state == PartyManagementDependency.RelationshipState.ACTIVE
      if (condition) {
        Future.successful(true)
      } else {
        Future.failed(SecurityOperatorRelationshipNotActive(relationshipId))
      }
    }

    for {
      relationship <- partyManagementService.getRelationshipById(relationshipId)(bearerToken)
      _            <- isActiveSecurityOperatorRelationship(relationship)
    } yield relationship
  }

  private[this] def securityOperatorRelationship(consumerId: UUID, operatorId: UUID)(
    bearerToken: String
  ): Future[Option[PartyManagementDependency.Relationship]] =
    for {
      relationships <-
        partyManagementService
          .getRelationships(consumerId, operatorId, Seq(PartyManagementService.PRODUCT_ROLE_SECURITY_OPERATOR))(
            bearerToken
          )
          .map(Some(_))
          // TODO This is dangerous because every error is treated as "missing party with given tax code"
          //  but currently there is no precise way to identify the error
          .recoverWith(_ => Future.successful(None))
      activeRelationships = relationships.toSeq.flatMap(
        _.items.filter(_.state == PartyManagementDependency.RelationshipState.ACTIVE)
      )
      securityOperatorRel = activeRelationships.headOption // Only one expected
    } yield securityOperatorRel

  private[this] def operatorsFromClient(client: AuthorizationmanagementDependency.Client)(
    bearerToken: String
  ): Future[Seq[Operator]] =
    client.relationships.toSeq.traverse(r => operatorFromRelationship(r)(bearerToken))

  private[this] def hasClientRelationship(
    client: authorizationmanagement.client.model.Client,
    relationshipId: UUID
  ): Future[UUID] =
    client.relationships
      .find(r => r == relationshipId)
      .toFuture(SecurityOperatorRelationshipNotFound(client.consumerId, relationshipId))

  private[this] def operatorFromRelationship(relationshipId: UUID)(bearerToken: String): Future[Operator] =
    for {
      relationship  <- partyManagementService.getRelationshipById(relationshipId)(bearerToken)
      user          <- userRegistryManagementService.getUserById(relationship.from)
      operatorState <- relationshipStateToApi(relationship.state).toFuture
    } yield Operator(
      relationshipId = relationship.id,
      taxCode = user.externalId,
      name = user.name,
      surname = user.surname,
      role = relationshipRoleToApi(relationship.role),
      product = relationshipProductToApi(relationship.product),
      state = operatorState
    )

  private[this] def clientToApi(
    client: AuthorizationmanagementDependency.Client,
    consumer: PartyManagementDependency.Organization,
    operator: Seq[Operator]
  ): Client =
    Client(
      id = client.id,
      consumer = PartyManagementService.organizationToApi(consumer),
      name = client.name,
      purposes = client.purposes.map(AuthorizationManagementService.purposeToApi),
      description = client.description,
      operators = Some(operator),
      kind = AuthorizationManagementService.convertToApiClientKind(client.kind)
    )

  /** Code: 200, Message: returns the corresponding base 64 encoded key, DataType: EncodedClientKey
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Key not found, DataType: Problem
    * Code: 500, Message: Internal Server Error, DataType: Problem
    */
  override def getEncodedClientKeyById(clientId: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerEncodedClientKey: ToEntityMarshaller[EncodedClientKey],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    logger.info("Getting encoded client {} key by key id {}", clientId, keyId)
    val result = for {
      bearerToken <- getFutureBearer(contexts)
      clientUuid  <- clientId.toFutureUUID
      encodedKey  <- authorizationManagementService.getEncodedClientKey(clientUuid, keyId)(bearerToken)
    } yield EncodedClientKey(key = encodedKey.key)

    onComplete(result) {
      case Success(key) => getEncodedClientKeyById200(key)
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error("Error while getting encoded client {} key by key id {}", clientId, keyId, ex)
        getClientKeyById404(
          problemOf(StatusCodes.NotFound, ResourceNotFoundError(s"Client id $clientId - key id: $keyId"))
        )
      case Failure(ex) =>
        logger.error("Error while getting encoded client {} key by key id {}", clientId, keyId, ex)
        internalServerError(problemOf(StatusCodes.InternalServerError, EncodedClientKeyRetrievalError))
    }
  }

}
