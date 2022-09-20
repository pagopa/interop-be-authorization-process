package it.pagopa.interop.authorizationprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import cats.implicits._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop._
import it.pagopa.interop.agreementmanagement.client.{model => AgreementManagementDependency}
import it.pagopa.interop.authorizationmanagement.client.invoker.{ApiError => AuthorizationManagementApiError}
import it.pagopa.interop.authorizationmanagement.client.{model => AuthorizationManagementDependency}
import it.pagopa.interop.authorizationprocess.api.ClientApiService
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors._
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationprocess.service.PartyManagementService.{
  relationshipProductToApi,
  relationshipRoleToApi,
  relationshipStateToApi
}
import it.pagopa.interop.authorizationprocess.service._
import it.pagopa.interop.commons.utils.ORGANIZATION_ID_CLAIM
import it.pagopa.interop.catalogmanagement.client.{model => CatalogManagementDependency}
import it.pagopa.interop.commons.jwt.{ADMIN_ROLE, M2M_ROLE, SECURITY_ROLE}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.AkkaUtils._
import it.pagopa.interop.commons.utils.TypeConversions.{EitherOps, OptionOps, StringOps}
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.{
  MissingBearer,
  MissingUserId,
  ResourceNotFoundError
}
import it.pagopa.interop.purposemanagement.client.invoker.{ApiError => PurposeManagementApiError}
import it.pagopa.interop.purposemanagement.client.{model => PurposeManagementDependency}
import it.pagopa.interop.selfcare._
import it.pagopa.interop.selfcare.partymanagement.client.model.{Problem => _, _}
import it.pagopa.interop.selfcare.partymanagement.client.{model => PartyManagementDependency}
import it.pagopa.interop.selfcare.userregistry.client.model.UserResource

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

  val logger: LoggerTakingImplicit[ContextFieldsToLog] = Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  def internalServerError(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = complete((500, responseProblem))

  override def createConsumerClient(clientSeed: ClientSeed)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route = authorize(ADMIN_ROLE) {
    logger.info("Creating CONSUMER client {}", clientSeed.name)
    val result = for {
      organizationId <- getClaimFuture(contexts, ORGANIZATION_ID_CLAIM).flatMap(_.toFutureUUID)
      _ = logger.info("Creating CONSUMER client {} for consumer {}", clientSeed.name, organizationId)
      client    <- authorizationManagementService.createClient(
        organizationId,
        clientSeed.name,
        clientSeed.description,
        authorizationmanagement.client.model.ClientKind.CONSUMER
      )(contexts)
      apiClient <- getClient(client)
    } yield apiClient

    onComplete(result) {
      case Success(client)        => createConsumerClient201(client)
      case Failure(MissingUserId) =>
        logger.error("Error in creating client {}", clientSeed.name, MissingUserId)
        createConsumerClient401(problemOf(StatusCodes.Unauthorized, MissingUserId))
      case Failure(MissingBearer) =>
        logger.error("Error in creating client {}", clientSeed.name, MissingBearer)
        createConsumerClient401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex)            =>
        logger.error(s"Error in creating CONSUMER client ${clientSeed.name}", ex)
        internalServerError(problemOf(StatusCodes.InternalServerError, ClientCreationError))
    }
  }

  override def createApiClient(clientSeed: ClientSeed)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route = authorize(ADMIN_ROLE) {
    logger.info("Creating API client {}", clientSeed.name)
    val result = for {
      organizationId <- getClaimFuture(contexts, ORGANIZATION_ID_CLAIM).flatMap(_.toFutureUUID)
      _ = logger.info("Creating API client {} for and consumer {}", clientSeed.name, organizationId)
      client    <- authorizationManagementService.createClient(
        organizationId,
        clientSeed.name,
        clientSeed.description,
        authorizationmanagement.client.model.ClientKind.API
      )(contexts)
      apiClient <- getClient(client)
    } yield apiClient

    onComplete(result) {
      case Success(client)        => createApiClient201(client)
      case Failure(MissingUserId) =>
        logger.error("Error in creating API client {}", clientSeed.name, MissingUserId)
        createApiClient401(problemOf(StatusCodes.Unauthorized, MissingUserId))
      case Failure(MissingBearer) =>
        logger.error("Error in creating API client {}", clientSeed.name, MissingBearer)
        createApiClient401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex)            =>
        logger.error(s"Error in creating client ${clientSeed.name}", ex)
        internalServerError(problemOf(StatusCodes.InternalServerError, ClientCreationError))
    }
  }

  override def getClient(clientId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, M2M_ROLE) {
    logger.info("Getting client {}", clientId)
    val result: Future[Client] = for {
      clientUuid     <- clientId.toFutureUUID
      organizationId <- getClaimFuture(contexts, ORGANIZATION_ID_CLAIM).flatMap(_.toFutureUUID)
      client         <- authorizationManagementService.getClient(clientUuid)(contexts)
      apiClient      <- isConsumerOrProducer(organizationId, client).ifM(
        getClient(client),
        InvalidOrganization(clientId, organizationId.toString()).raiseError[Future, Client]
      )
    } yield apiClient

    onComplete(result) {
      case Success(client)                                                   => getClient200(client)
      case Failure(MissingUserId)                                            =>
        logger.error(s"Error in getting client $clientId", MissingUserId)
        getClient401(problemOf(StatusCodes.Unauthorized, MissingUserId))
      case Failure(MissingBearer)                                            =>
        logger.error(s"Error in getting client $clientId", MissingBearer)
        getClient401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: InvalidOrganization)                                  =>
        logger.error(s"Error in getting client $clientId", ex)
        getClient403(problemOf(StatusCodes.Forbidden, ex))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error(s"Error in getting client $clientId", ex)
        getClient404(problemOf(StatusCodes.NotFound, ResourceNotFoundError(s"Client id $clientId")))
      case Failure(ex)                                                       =>
        logger.error(s"Error in getting client $clientId", ex)
        internalServerError(problemOf(StatusCodes.InternalServerError, ClientRetrievalError))
    }
  }

  override def listClients(
    offset: Option[Int],
    limit: Option[Int],
    consumerId: String,
    purposeId: Option[String],
    kind: Option[String]
  )(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClients: ToEntityMarshaller[Clients],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, M2M_ROLE) {
    logger.info(
      s"Listing clients (offset: $offset and limit: $limit) for purposeId $purposeId and consumer $consumerId of kind $kind"
    )

    // TODO Improve multiple requests
    val result: Future[Seq[Client]] = for {
      personId          <- getUidFuture(contexts).flatMap(_.toFutureUUID)
      consumerUuid      <- consumerId.toFutureUUID
      clientKind        <- kind.traverse(AuthorizationManagementDependency.ClientKind.fromValue).toFuture
      relationships     <-
        partyManagementService
          .getRelationships(
            consumerUuid,
            personId,
            Seq(PartyManagementService.PRODUCT_ROLE_SECURITY_OPERATOR, PartyManagementService.PRODUCT_ROLE_ADMIN)
          )
          .map(_.items.filter(_.state == RelationshipState.ACTIVE))
      purposeUuid       <- purposeId.traverse(_.toFutureUUID)
      managementClients <-
        if (relationships.exists(_.product.role == PartyManagementService.PRODUCT_ROLE_ADMIN))
          authorizationManagementService.listClients(
            offset = offset,
            limit = limit,
            relationshipId = None,
            consumerId = consumerUuid.some,
            purposeId = purposeUuid,
            kind = clientKind
          )(contexts)
        else
          Future
            .traverse(relationships.map(_.id.some))(relationshipId =>
              authorizationManagementService.listClients(
                offset = offset,
                limit = limit,
                relationshipId = relationshipId,
                consumerId = consumerUuid.some,
                purposeId = purposeUuid,
                kind = clientKind
              )(contexts)
            )
            .map(_.flatten)

      clients <- Future.traverse(managementClients)(getClient)
    } yield clients

    onComplete(result) {
      case Success(clients)                 => listClients200(Clients(clients))
      case Failure(ex: UUIDConversionError) =>
        logger.error(
          s"Error while listing clients (offset: $offset and limit: $limit) for purposeId $purposeId and consumer $consumerId of kind $kind",
          ex
        )
        listClients400(problemOf(StatusCodes.BadRequest, ex))
      case Failure(MissingUserId)           =>
        logger.error(
          s"Error while listing clients (offset: $offset and limit: $limit) for purposeId $purposeId and consumer $consumerId of kind $kind",
          MissingUserId
        )
        listClients401(problemOf(StatusCodes.Unauthorized, MissingUserId))
      case Failure(MissingBearer)           =>
        logger.error(
          s"Error while listing clients (offset: $offset and limit: $limit) for purposeId $purposeId and consumer $consumerId of kind $kind",
          MissingBearer
        )
        listClients401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex)                      =>
        logger.error(
          s"Error while listing clients (offset: $offset and limit: $limit) for purposeId $purposeId and consumer $consumerId of kind $kind",
          ex
        )
        internalServerError(problemOf(StatusCodes.InternalServerError, ClientListingError))
    }
  }

  override def deleteClient(
    clientId: String
  )(implicit contexts: Seq[(String, String)], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    authorize(ADMIN_ROLE) {
      logger.info("Deleting client {}", clientId)
      val result = for {
        clientUuid <- clientId.toFutureUUID
        _          <- checkAuthorization(clientUuid)
        _          <- authorizationManagementService.deleteClient(clientUuid)(contexts)
      } yield ()

      onComplete(result) {
        case Success(_)                                                        => deleteClient204
        case Failure(MissingBearer)                                            =>
          logger.error(s"Error while deleting client $clientId", MissingBearer)
          deleteClient401(problemOf(StatusCodes.Unauthorized, MissingBearer))
        case Failure(ex: InvalidOrganization)                                  =>
          logger.error(s"Error in getting client $clientId", ex)
          deleteClient403(problemOf(StatusCodes.Forbidden, ex))
        case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
          logger.error(s"Error while deleting client $clientId", ex)
          deleteClient404(problemOf(StatusCodes.NotFound, ResourceNotFoundError(s"Client id $clientId")))
        case Failure(ex)                                                       =>
          logger.error(s"Error while deleting client $clientId", ex)
          internalServerError(problemOf(StatusCodes.InternalServerError, ClientDeletionError))
      }
    }

  override def clientOperatorRelationshipBinding(clientId: String, relationshipId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route = authorize(ADMIN_ROLE) {
    logger.info("Binding client {} with relationship {}", clientId, relationshipId)
    val result = for {
      clientUUID       <- clientId.toFutureUUID
      relationshipUUID <- relationshipId.toFutureUUID
      organizationId   <- getClaimFuture(contexts, ORGANIZATION_ID_CLAIM).flatMap(_.toFutureUUID)
      client           <- authorizationManagementService
        .getClient(clientUUID)(contexts)
        .ensureOr(client => InvalidOrganization(s"client $clientId", client.consumerId.toString()))(
          _.consumerId == organizationId
        )
      relationship     <- getSecurityRelationship(relationshipUUID)
      updatedClient    <- client.relationships
        .find(_ === relationship.id)
        .fold(authorizationManagementService.addRelationship(clientUUID, relationship.id)(contexts))(_ =>
          Future.failed(OperatorRelationshipAlreadyAssigned(client.id, relationship.id))
        )
      apiClient        <- getClient(updatedClient)
    } yield apiClient

    onComplete(result) {
      case Success(client)                                    => clientOperatorRelationshipBinding201(client)
      case Failure(MissingUserId)                             =>
        logger.error(s"Error while binding client $clientId with relationship $relationshipId", MissingUserId)
        clientOperatorRelationshipBinding401(problemOf(StatusCodes.Unauthorized, MissingUserId))
      case Failure(MissingBearer)                             =>
        logger.error(s"Error while binding client $clientId with relationship $relationshipId", MissingBearer)
        clientOperatorRelationshipBinding401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: UUIDConversionError)                   =>
        logger.error(s"Error while binding client $clientId with relationship $relationshipId", ex)
        clientOperatorRelationshipBinding400(problemOf(StatusCodes.BadRequest, ex))
      case Failure(ex: SecurityOperatorRelationshipNotActive) =>
        logger.error(s"Error while binding client $clientId with relationship $relationshipId", ex)
        clientOperatorRelationshipBinding400(problemOf(StatusCodes.BadRequest, ex))
      case Failure(ex: OperatorRelationshipAlreadyAssigned)   =>
        logger.error(s"Error while binding client $clientId with relationship $relationshipId", ex)
        clientOperatorRelationshipBinding400(problemOf(StatusCodes.BadRequest, ex))
      case Failure(ex: InvalidOrganization)                   =>
        logger.error(s"Error in getting client $clientId", ex)
        clientOperatorRelationshipBinding403(problemOf(StatusCodes.Forbidden, ex))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error(s"Error while binding client $clientId with relationship $relationshipId", ex)
        clientOperatorRelationshipBinding404(
          problemOf(
            StatusCodes.NotFound,
            ResourceNotFoundError(s"Client id $clientId - relationship id: $relationshipId")
          )
        )
      case Failure(ex)                                                       =>
        logger.error(s"Error while binding client $clientId with relationship $relationshipId", ex)
        internalServerError(problemOf(StatusCodes.InternalServerError, OperatorAdditionError))
    }
  }

  override def removeClientOperatorRelationship(clientId: String, relationshipId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE) {
    logger.info("Removing binding between client {} with relationship {}", clientId, relationshipId)
    val result = for {
      userId                 <- getUidFuture(contexts)
      userUUID               <- userId.toFutureUUID
      clientUUID             <- clientId.toFutureUUID
      _                      <- checkAuthorization(clientUUID)
      relationshipUUID       <- relationshipId.toFutureUUID
      requesterRelationships <- partyManagementService.getRelationshipsByPersonId(userUUID, Seq.empty)
      _                      <- Future
        .failed(UserNotAllowedToRemoveOwnRelationship(clientId, relationshipId))
        .whenA(isNotRemovable(relationshipUUID)(requesterRelationships))
      _ <- authorizationManagementService.removeClientRelationship(clientUUID, relationshipUUID)(contexts)
    } yield ()

    onComplete(result) {
      case Success(_)                                                        => removeClientOperatorRelationship204
      case Failure(MissingUserId)                                            =>
        logger.error(s"Removing binding between client $clientId with relationship $relationshipId", MissingUserId)
        removeClientOperatorRelationship401(problemOf(StatusCodes.Unauthorized, MissingUserId))
      case Failure(MissingBearer)                                            =>
        logger.error(s"Removing binding between client $clientId with relationship $relationshipId", MissingBearer)
        removeClientOperatorRelationship401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: UUIDConversionError)                                  =>
        logger.error(s"Removing binding between client $clientId with relationship $relationshipId", ex)
        removeClientOperatorRelationship400(problemOf(StatusCodes.BadRequest, ex))
      case Failure(ex: UserNotAllowedToRemoveOwnRelationship)                =>
        logger.error(s"Removing binding between client $clientId with relationship $relationshipId", ex)
        removeClientOperatorRelationship403(problemOf(StatusCodes.Forbidden, ex))
      case Failure(ex: InvalidOrganization)                                  =>
        logger.error(s"Error in getting client $clientId", ex)
        removeClientOperatorRelationship403(problemOf(StatusCodes.Forbidden, ex))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error(s"Removing binding between client $clientId with relationship $relationshipId", ex)
        removeClientOperatorRelationship404(
          problemOf(
            StatusCodes.NotFound,
            ResourceNotFoundError(s"Client id $clientId - relationship id: $relationshipId")
          )
        )
      case Failure(ex)                                                       =>
        logger.error(s"Removing binding between client $clientId with relationship $relationshipId", ex)
        internalServerError(problemOf(StatusCodes.InternalServerError, OperatorRemovalError))
    }
  }

  private def isNotRemovable(relationshipId: UUID): Relationships => Boolean = relationships => {
    relationships.items.exists(relationship =>
      relationship.id == relationshipId &&
        relationship.product.role != PartyManagementService.PRODUCT_ROLE_ADMIN
    )
  }

  override def getClientKeyById(clientId: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerReadClientKey: ToEntityMarshaller[ReadClientKey]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, M2M_ROLE) {
    logger.info("Getting client {} key by id {}", clientId, keyId)
    val result = for {
      clientUuid <- clientId.toFutureUUID
      _          <- checkAuthorization(clientUuid)
      key        <- authorizationManagementService.getKey(clientUuid, keyId)
      operator   <- operatorFromRelationship(key.relationshipId)
    } yield AuthorizationManagementService.readKeyToApi(key, operator)

    onComplete(result) {
      case Success(readKey)                                                  => getClientKeyById200(readKey)
      case Failure(MissingUserId)                                            =>
        logger.error(s"Error while getting client $clientId key by id $keyId", MissingUserId)
        getClientKeyById401(problemOf(StatusCodes.Unauthorized, MissingUserId))
      case Failure(MissingBearer)                                            =>
        logger.error(s"Error while getting client $clientId key by id $keyId", MissingBearer)
        getClientKeyById401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: InvalidOrganization)                                  =>
        logger.error(s"Error in getting client $clientId", ex)
        getClientKeyById403(problemOf(StatusCodes.Forbidden, ex))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error(s"Error while getting client $clientId key by id $keyId", ex)
        getClientKeyById404(
          problemOf(StatusCodes.NotFound, ResourceNotFoundError(s"Client id $clientId - key id: $keyId"))
        )
      case Failure(ex)                                                       =>
        logger.error(s"Error while getting client $clientId key by id $keyId", ex)
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
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE) {
    logger.info("Deleting client {} key by id {}", clientId, keyId)
    val result = for {
      clientUuid <- clientId.toFutureUUID
      _          <- checkAuthorization(clientUuid)
      _          <- authorizationManagementService.deleteKey(clientUuid, keyId)(contexts)
    } yield ()

    onComplete(result) {
      case Success(_)                                                        => deleteClientKeyById204
      case Failure(MissingBearer)                                            =>
        logger.error(s"Error while deleting client $clientId key by id $keyId", MissingBearer)
        deleteClientKeyById401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error(s"Error while deleting client $clientId key by id $keyId", ex)
        deleteClientKeyById404(
          problemOf(StatusCodes.NotFound, ResourceNotFoundError(s"Client id $clientId - key id: $keyId"))
        )
      case Failure(ex: InvalidOrganization)                                  =>
        logger.error(s"Error in getting client $clientId", ex)
        deleteClientKeyById403(problemOf(StatusCodes.Forbidden, ex))
      case Failure(ex)                                                       =>
        logger.error(s"Error while deleting client $clientId key by id $keyId", ex)
        internalServerError(problemOf(StatusCodes.InternalServerError, ClientKeyDeletionError))
    }
  }

  override def createKeys(clientId: String, keysSeeds: Seq[KeySeed])(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE) {
    logger.info("Creating keys for client {}", clientId)
    val result = for {
      userId         <- getUidFuture(contexts) >>= (_.toFutureUUID)
      clientUuid     <- clientId.toFutureUUID
      organizationId <- getClaimFuture(contexts, ORGANIZATION_ID_CLAIM).flatMap(_.toFutureUUID)
      client         <- authorizationManagementService
        .getClient(clientUuid)(contexts)
        .ensureOr(client => InvalidOrganization(s"client $clientId", client.consumerId.toString()))(
          _.consumerId == organizationId
        )
      relationshipId <- securityOperatorRelationship(client.consumerId, userId).map(_.id)
      seeds = keysSeeds.map(AuthorizationManagementService.toDependencyKeySeed(_, relationshipId))
      keysResponse <- authorizationManagementService.createKeys(clientUuid, seeds)(contexts)
    } yield ClientKeys(keysResponse.keys.map(AuthorizationManagementService.keyToApi))

    onComplete(result) {
      case Success(keys)                                                     => createKeys201(keys)
      case Failure(MissingUserId)                                            =>
        logger.error(s"Error while creating keys for client $clientId", MissingUserId)
        createKeys401(problemOf(StatusCodes.Unauthorized, MissingUserId))
      case Failure(MissingBearer)                                            =>
        logger.error(s"Error while creating keys for client $clientId", MissingBearer)
        createKeys401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: EnumParameterError)                                   =>
        logger.error(s"Error while creating keys for client $clientId", ex)
        createKeys400(problemOf(StatusCodes.BadRequest, ex))
      case Failure(ex: SecurityOperatorRelationshipNotFound)                 =>
        logger.error(s"Error while creating keys for client $clientId", ex)
        createKeys403(problemOf(StatusCodes.Forbidden, ex))
      case Failure(ex: InvalidOrganization)                                  =>
        logger.error(s"Error in getting client $clientId", ex)
        createKeys403(problemOf(StatusCodes.Forbidden, ex))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error(s"Error while creating keys for client $clientId", ex)
        createKeys404(problemOf(StatusCodes.NotFound, ResourceNotFoundError(s"Client id $clientId")))
      case Failure(ex)                                                       =>
        logger.error(s"Error while creating keys for client $clientId", ex)
        internalServerError(problemOf(StatusCodes.InternalServerError, ClientKeyCreationError))
    }
  }

  override def getClientKeys(clientId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientKeys: ToEntityMarshaller[ReadClientKeys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, M2M_ROLE) {
    logger.info("Getting keys of client {}", clientId)
    val result = for {
      clientUuid   <- clientId.toFutureUUID
      _            <- checkAuthorization(clientUuid)
      keysResponse <- authorizationManagementService.getClientKeys(clientUuid)(contexts)
      keys         <- Future.traverse(keysResponse.keys)(k =>
        operatorFromRelationship(k.relationshipId).map(operator =>
          AuthorizationManagementService.readKeyToApi(k, operator)
        )
      )
    } yield ReadClientKeys(keys)

    onComplete(result) {
      case Success(keys)                                                     => getClientKeys200(keys)
      case Failure(MissingUserId)                                            =>
        logger.error(s"Error while getting keys of client $clientId", MissingUserId)
        getClientKeys401(problemOf(StatusCodes.Unauthorized, MissingUserId))
      case Failure(MissingBearer)                                            =>
        logger.error(s"Error while getting keys of client $clientId", MissingBearer)
        getClientKeys401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: InvalidOrganization)                                  =>
        logger.error(s"Error in getting client $clientId", ex)
        getClientKeys403(problemOf(StatusCodes.Forbidden, ex))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error(s"Error while getting keys of client $clientId", ex)
        getClientKeys404(problemOf(StatusCodes.NotFound, ResourceNotFoundError(s"Client id $clientId ")))
      case Failure(ex)                                                       =>
        logger.error(s"Error while getting keys of client $clientId", ex)
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
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE) {
    logger.info("Getting operators of client {}", clientId)
    val result = for {
      clientUuid     <- clientId.toFutureUUID
      organizationId <- getClaimFuture(contexts, ORGANIZATION_ID_CLAIM).flatMap(_.toFutureUUID)
      client         <- authorizationManagementService
        .getClient(clientUuid)(contexts)
        .ensureOr(client => InvalidOrganization(s"client $clientId", client.consumerId.toString()))(
          _.consumerId == organizationId
        )
      operators      <- operatorsFromClient(client)
    } yield operators

    onComplete(result) {
      case Success(keys)                                                     => getClientOperators200(keys)
      case Failure(MissingUserId)                                            =>
        logger.error(s"Error while getting operators of client $clientId", MissingUserId)
        getClientOperators401(problemOf(StatusCodes.Unauthorized, MissingUserId))
      case Failure(MissingBearer)                                            =>
        logger.error(s"Error while getting operators of client $clientId", MissingBearer)
        getClientOperators401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: InvalidOrganization)                                  =>
        logger.error(s"Error in getting client $clientId", ex)
        getClientOperators403(problemOf(StatusCodes.Forbidden, ex))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error(s"Error while getting operators of client $clientId", ex)
        getClientOperators404(problemOf(StatusCodes.NotFound, ResourceNotFoundError(s"Client id $clientId ")))
      case Failure(ex)                                                       =>
        logger.error(s"Error while getting operators of client $clientId", ex)
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
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, M2M_ROLE) {
    logger.info("Getting operators of client {} by relationship {}", clientId, relationshipId)
    val result = for {
      clientUUID       <- clientId.toFutureUUID
      organizationId   <- getClaimFuture(contexts, ORGANIZATION_ID_CLAIM).flatMap(_.toFutureUUID)
      relationshipUUID <- relationshipId.toFutureUUID
      client           <- authorizationManagementService
        .getClient(clientUUID)(contexts)
        .ensureOr(client => InvalidOrganization(s"client $clientId", client.consumerId.toString()))(
          _.consumerId == organizationId
        )
      _                <- hasClientRelationship(client, relationshipUUID)
      operator         <- operatorFromRelationship(relationshipUUID)
    } yield operator

    onComplete(result) {
      case Success(operator)      => getClientOperatorRelationshipById200(operator)
      case Failure(MissingUserId) =>
        logger.error(
          s"Error while getting operators of client $clientId by relationship $relationshipId",
          MissingUserId
        )
        getClientOperatorRelationshipById401(problemOf(StatusCodes.Unauthorized, MissingUserId))
      case Failure(MissingBearer) =>
        logger.error(
          s"Error while getting operators of client $clientId by relationship $relationshipId",
          MissingBearer
        )
        getClientOperatorRelationshipById401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error(s"Error while getting operators of client $clientId by relationship $relationshipId", ex)
        getClientOperatorRelationshipById404(
          problemOf(
            StatusCodes.NotFound,
            ResourceNotFoundError(s"Client id $clientId - relationship id: $relationshipId")
          )
        )
      case Failure(ex: InvalidOrganization)                                  =>
        logger.error(s"Error in getting client $clientId", ex)
        getClientOperatorRelationshipById403(problemOf(StatusCodes.Forbidden, ex))
      case Failure(ex: SecurityOperatorRelationshipNotFound)                 =>
        logger.error(s"Error while getting operators of client $clientId by relationship $relationshipId", ex)
        getClientOperatorRelationshipById404(problemOf(StatusCodes.NotFound, ex))
      case Failure(ex)                                                       =>
        logger.error(s"Error while getting operators of client $clientId by relationship $relationshipId", ex)
        internalServerError(problemOf(StatusCodes.InternalServerError, ClientOperatorsRelationshipRetrievalError))
    }
  }

  override def addClientPurpose(clientId: String, details: PurposeAdditionDetails)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE) {
    logger.info("Adding Purpose {} to Client {}", details.purposeId, clientId)

    val validAgreementsStates: Set[AgreementManagementDependency.AgreementState] =
      Set[AgreementManagementDependency.AgreementState](
        AgreementManagementDependency.AgreementState.ACTIVE,
        AgreementManagementDependency.AgreementState.SUSPENDED
      )

    def descriptorToComponentState(
      descriptor: CatalogManagementDependency.EServiceDescriptor
    ): AuthorizationManagementDependency.ClientComponentState = descriptor.state match {
      case CatalogManagementDependency.EServiceDescriptorState.PUBLISHED =>
        AuthorizationManagementDependency.ClientComponentState.ACTIVE
      case _ => AuthorizationManagementDependency.ClientComponentState.INACTIVE
    }

    def agreementToComponentState(
      agreement: AgreementManagementDependency.Agreement
    ): AuthorizationManagementDependency.ClientComponentState = agreement.state match {
      case AgreementManagementDependency.AgreementState.ACTIVE =>
        AuthorizationManagementDependency.ClientComponentState.ACTIVE
      case _ => AuthorizationManagementDependency.ClientComponentState.INACTIVE
    }

    def purposeVersionToComponentState(
      purposeVersion: PurposeManagementDependency.PurposeVersion
    ): AuthorizationManagementDependency.ClientComponentState = purposeVersion.state match {
      case PurposeManagementDependency.PurposeVersionState.ACTIVE =>
        AuthorizationManagementDependency.ClientComponentState.ACTIVE
      case _ => AuthorizationManagementDependency.ClientComponentState.INACTIVE
    }

    val result: Future[Unit] = for {
      clientUuid <- clientId.toFutureUUID
      purpose    <- purposeManagementService.getPurpose(details.purposeId)
      eService   <- catalogManagementService.getEService(purpose.eserviceId)
      agreements <- agreementManagementService.getAgreements(purpose.eserviceId, purpose.consumerId)
      agreement  <- agreements
        .filter(agreement => validAgreementsStates.contains(agreement.state))
        .maxByOption(_.createdAt)
        .toFuture(ClientPurposeAddAgreementNotFound(purpose.eserviceId.toString, purpose.consumerId.toString))
      descriptor <- eService.descriptors
        .find(_.id == agreement.descriptorId)
        .toFuture(ClientPurposeAddDescriptorNotFound(purpose.eserviceId.toString, agreement.descriptorId.toString))
      version    <- purpose.versions
        .maxByOption(_.createdAt)
        .toFuture(ClientPurposeAddPurposeVersionNotFound(purpose.id.toString))
      states = AuthorizationManagementDependency.ClientStatesChainSeed(
        eservice = AuthorizationManagementDependency.ClientEServiceDetailsSeed(
          eserviceId = eService.id,
          descriptorId = descriptor.id,
          state = descriptorToComponentState(descriptor),
          audience = descriptor.audience,
          voucherLifespan = descriptor.voucherLifespan
        ),
        agreement = AuthorizationManagementDependency
          .ClientAgreementDetailsSeed(
            eserviceId = agreement.eserviceId,
            consumerId = agreement.consumerId,
            agreementId = agreement.id,
            state = agreementToComponentState(agreement)
          ),
        purpose = AuthorizationManagementDependency.ClientPurposeDetailsSeed(
          purposeId = purpose.id,
          versionId = version.id,
          state = purposeVersionToComponentState(version)
        )
      )
      seed   = AuthorizationManagementDependency.PurposeSeed(states)
      _ <- authorizationManagementService.addClientPurpose(clientUuid, seed)(contexts)
    } yield ()

    onComplete(result) {
      case Success(_)                                                  => addClientPurpose204
      case Failure(ex: PurposeManagementApiError[_]) if ex.code == 404 =>
        logger.error(s"Error adding purpose ${details.purposeId} to client $clientId", ex)
        createKeys404(problemOf(StatusCodes.NotFound, ResourceNotFoundError(s"Purpose id ${details.purposeId}")))
      case Failure(ex: ClientPurposeAddAgreementNotFound)              =>
        logger.error(s"Error adding purpose ${details.purposeId} to client $clientId - No valid Agreement found", ex)
        createKeys404(problemOf(StatusCodes.NotFound, ex))
      case Failure(ex: ClientPurposeAddPurposeVersionNotFound)         =>
        logger.error(s"Error adding purpose ${details.purposeId} to client $clientId - No valid Purpose found", ex)
        createKeys404(problemOf(StatusCodes.NotFound, ex))
      case Failure(ex)                                                 =>
        logger.error(s"Error adding purpose ${details.purposeId} to client $clientId", ex)
        internalServerError(
          problemOf(StatusCodes.InternalServerError, ClientPurposeAddError(clientId, details.purposeId.toString))
        )
    }
  }

  override def removeClientPurpose(clientId: String, purposeId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE) {
    logger.info("Removing Purpose from Client {}", clientId)

    val result: Future[Unit] = for {
      clientUuid  <- clientId.toFutureUUID
      _           <- checkAuthorization(clientUuid)
      purposeUuid <- purposeId.toFutureUUID
      _           <- authorizationManagementService.removeClientPurpose(clientUuid, purposeUuid)(contexts)
    } yield ()

    onComplete(result) {
      case Success(_)                                                  => addClientPurpose204
      case Failure(ex: InvalidOrganization)                            =>
        logger.error(s"Error in getting client $clientId", ex)
        createKeys403(problemOf(StatusCodes.Forbidden, ex))
      case Failure(ex: PurposeManagementApiError[_]) if ex.code == 404 =>
        logger.error(s"Error removing purpose $purposeId from client $clientId", ex)
        createKeys404(problemOf(StatusCodes.NotFound, ResourceNotFoundError(s"Purpose id $purposeId")))
      case Failure(ex)                                                 =>
        logger.error(s"Error removing purpose $purposeId from client $clientId", ex)
        internalServerError(problemOf(StatusCodes.InternalServerError, ClientPurposeRemoveError(clientId, purposeId)))
    }
  }

  private[this] def getClient(
    client: AuthorizationManagementDependency.Client
  )(implicit contexts: Seq[(String, String)]): Future[Client] = {
    def getLatestAgreement(
      purpose: AuthorizationManagementDependency.Purpose
    ): Future[AgreementManagementDependency.Agreement] = {
      val eServiceId: UUID = purpose.states.eservice.eserviceId
      for {
        agreements <- agreementManagementService.getAgreements(eServiceId, client.consumerId)
        client     <- agreements
          .sortBy(_.createdAt)
          .lastOption
          .toFuture(AgreementNotFound(eServiceId.toString, client.consumerId.toString))
      } yield client
    }

    def enrichPurpose(
      clientPurpose: AuthorizationManagementDependency.Purpose,
      agreement: AgreementManagementDependency.Agreement
    ): Future[
      (
        AuthorizationManagementDependency.Purpose,
        PurposeManagementDependency.Purpose,
        AgreementManagementDependency.Agreement,
        CatalogManagementDependency.EService,
        CatalogManagementDependency.EServiceDescriptor
      )
    ] = for {
      purpose    <- purposeManagementService.getPurpose(clientPurpose.states.purpose.purposeId)
      eService   <- catalogManagementService.getEService(agreement.eserviceId)
      descriptor <- eService.descriptors
        .find(_.id == agreement.descriptorId)
        .toFuture(DescriptorNotFound(agreement.eserviceId.toString, agreement.descriptorId.toString))
    } yield (clientPurpose, purpose, agreement, eService, descriptor)

    for {
      consumer              <- partyManagementService.getInstitution(client.consumerId)
      operators             <- operatorsFromClient(client)
      purposesAndAgreements <- Future.traverse(client.purposes)(purpose =>
        getLatestAgreement(purpose).map((purpose, _))
      )
      purposesDetails       <- Future.traverse(purposesAndAgreements) { case (p, a) => enrichPurpose(p, a) }
    } yield clientToApi(client, consumer, purposesDetails, operators)
  }

  private[this] def getSecurityRelationship(
    relationshipId: UUID
  )(implicit contexts: Seq[(String, String)]): Future[partymanagement.client.model.Relationship] = {

    def isActiveSecurityOperatorRelationship(relationship: Relationship): Future[Boolean] = {

      val isValidProductRole: Boolean =
        Set(PartyManagementService.PRODUCT_ROLE_SECURITY_OPERATOR, PartyManagementService.PRODUCT_ROLE_ADMIN)
          .contains(relationship.product.role)

      val isValidPartyRole: Boolean = Set[PartyRole](
        PartyManagementDependency.PartyRole.MANAGER,
        PartyManagementDependency.PartyRole.DELEGATE,
        PartyManagementDependency.PartyRole.OPERATOR
      ).contains(relationship.role)

      val isActive: Boolean = relationship.state == PartyManagementDependency.RelationshipState.ACTIVE

      val condition: Boolean = isValidProductRole && isValidPartyRole && isActive

      if (condition) Future.successful(true)
      else Future.failed(SecurityOperatorRelationshipNotActive(relationshipId))
    }

    for {
      relationship <- partyManagementService.getRelationshipById(relationshipId)
      _            <- isActiveSecurityOperatorRelationship(relationship)
    } yield relationship
  }

  private[this] def securityOperatorRelationship(consumerId: UUID, userId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[PartyManagementDependency.Relationship] = for {
    relationships <- partyManagementService
      .getRelationships(
        consumerId,
        userId,
        Seq(PartyManagementService.PRODUCT_ROLE_SECURITY_OPERATOR, PartyManagementService.PRODUCT_ROLE_ADMIN)
      )
    activeRelationShips = relationships.items.toList.filter(
      _.state == PartyManagementDependency.RelationshipState.ACTIVE
    )
    relationShip <- activeRelationShips.headOption.toFuture(SecurityOperatorRelationshipNotFound(consumerId, userId))
  } yield relationShip

  private[this] def operatorsFromClient(client: AuthorizationManagementDependency.Client)(implicit
    contexts: Seq[(String, String)]
  ): Future[Seq[Operator]] = Future.traverse(client.relationships.toList)(operatorFromRelationship)

  private[this] def hasClientRelationship(
    client: authorizationmanagement.client.model.Client,
    relationshipId: UUID
  ): Future[UUID] =
    client.relationships
      .find(r => r == relationshipId)
      .toFuture(SecurityOperatorRelationshipNotFound(client.consumerId, relationshipId))

  private[this] def operatorFromRelationship(
    relationshipId: UUID
  )(implicit contexts: Seq[(String, String)]): Future[Operator] =
    for {
      relationship <- partyManagementService.getRelationshipById(relationshipId)
      user         <- userRegistryManagementService.getUserById(relationship.from)
      userInfo     <- extractUserInfo(user)
      (name, familyName, taxCode) = userInfo
      operatorState <- relationshipStateToApi(relationship.state).toFuture
    } yield Operator(
      relationshipId = relationship.id,
      taxCode = taxCode,
      name = name,
      familyName = familyName,
      role = relationshipRoleToApi(relationship.role),
      product = relationshipProductToApi(relationship.product),
      state = operatorState
    )

  def extractUserInfo(user: UserResource): Future[(String, String, String)] = {
    val userInfo = for {
      name       <- user.name
      familyName <- user.familyName
      fiscalCode <- user.fiscalCode
    } yield (name.value, familyName.value, fiscalCode)

    userInfo.toFuture(MissingUserInfo(user.id))
  }

  def isConsumerOrProducer(organizationId: UUID, client: ManagementClient)(implicit
    ec: ExecutionContext,
    contexts: Seq[(String, String)]
  ): Future[Boolean] = {
    def isProducer(): Future[Boolean] = Future
      .traverse(client.purposes.map(_.states.eservice.eserviceId))(catalogManagementService.getEService)
      .map(eservices => eservices.map(_.producerId).contains(organizationId))

    (client.consumerId == organizationId).pure[Future].ifM(true.pure[Future], isProducer())
  }

  private[this] def clientToApi(
    client: AuthorizationManagementDependency.Client,
    consumer: PartyManagementDependency.Institution,
    purposesDetails: Seq[
      (
        AuthorizationManagementDependency.Purpose,
        PurposeManagementDependency.Purpose,
        AgreementManagementDependency.Agreement,
        CatalogManagementDependency.EService,
        CatalogManagementDependency.EServiceDescriptor
      )
    ],
    operator: Seq[Operator]
  ): Client = {
    def purposeToApi(
      clientPurpose: AuthorizationManagementDependency.Purpose,
      purpose: PurposeManagementDependency.Purpose,
      agreement: AgreementManagementDependency.Agreement,
      eService: CatalogManagementDependency.EService,
      descriptor: CatalogManagementDependency.EServiceDescriptor
    ): Purpose = {
      val apiEService   = CatalogManagementService.eServiceToApi(eService)
      val apiDescriptor = CatalogManagementService.descriptorToApi(descriptor)
      val apiAgreement  = AgreementManagementService.agreementToApi(agreement, apiEService, apiDescriptor)
      AuthorizationManagementService.purposeToApi(clientPurpose, purpose.title, apiAgreement)
    }

    Client(
      id = client.id,
      consumer = PartyManagementService.institutionToApi(consumer),
      name = client.name,
      purposes = purposesDetails.map(t => (purposeToApi _).tupled(t)),
      description = client.description,
      operators = Some(operator),
      kind = AuthorizationManagementService.convertToApiClientKind(client.kind)
    )
  }

  private[this] def checkAuthorization(clientId: UUID)(implicit contexts: Seq[(String, String)]): Future[Unit] =
    for {
      organizationId <- getClaimFuture(contexts, ORGANIZATION_ID_CLAIM).flatMap(_.toFutureUUID)
      _              <- authorizationManagementService
        .getClient(clientId)(contexts)
        .ensureOr(client => InvalidOrganization(s"client $clientId", client.consumerId.toString()))(
          _.consumerId == organizationId
        )
    } yield ()

  override def getEncodedClientKeyById(clientId: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerEncodedClientKey: ToEntityMarshaller[EncodedClientKey],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, M2M_ROLE) {
    logger.info("Getting encoded client {} key by key id {}", clientId, keyId)
    val result = for {
      clientUuid <- clientId.toFutureUUID
      _          <- checkAuthorization(clientUuid)
      encodedKey <- authorizationManagementService.getEncodedClientKey(clientUuid, keyId)(contexts)
    } yield EncodedClientKey(key = encodedKey.key)

    onComplete(result) {
      case Success(key)                                                      => getEncodedClientKeyById200(key)
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error(s"Error while getting encoded client $clientId key by key id $keyId", ex)
        getClientKeyById404(
          problemOf(StatusCodes.NotFound, ResourceNotFoundError(s"Client id $clientId - key id: $keyId"))
        )
      case Failure(ex: InvalidOrganization)                                  =>
        logger.error(s"Error in getting client $clientId", ex)
        getClientKeyById403(problemOf(StatusCodes.Forbidden, ex))
      case Failure(ex)                                                       =>
        logger.error(s"Error while getting encoded client $clientId key by key id $keyId", ex)
        internalServerError(problemOf(StatusCodes.InternalServerError, EncodedClientKeyRetrievalError))
    }
  }

}
