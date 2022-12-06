package it.pagopa.interop.authorizationprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import cats.implicits._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop._
import it.pagopa.interop.agreementmanagement.client.{model => AgreementManagementDependency}
import it.pagopa.interop.authorizationmanagement.client.{model => AuthorizationManagementDependency}
import it.pagopa.interop.authorizationprocess.api.ClientApiService
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors._
import it.pagopa.interop.authorizationprocess.error.ClientApiHandlers._
import it.pagopa.interop.authorizationprocess.error._
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationprocess.service.PartyManagementService.{
  relationshipProductToApi,
  relationshipRoleToApi,
  relationshipStateToApi
}
import it.pagopa.interop.authorizationprocess.service._
import it.pagopa.interop.catalogmanagement.client.{model => CatalogManagementDependency}
import it.pagopa.interop.commons.jwt.{ADMIN_ROLE, M2M_ROLE, SECURITY_ROLE, authorize}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.AkkaUtils.{getOrganizationIdFutureUUID, getUidFutureUUID}
import it.pagopa.interop.commons.utils.TypeConversions.{EitherOps, OptionOps, StringOps}
import it.pagopa.interop.purposemanagement.client.{model => PurposeManagementDependency}
import it.pagopa.interop.selfcare._
import it.pagopa.interop.selfcare.partymanagement.client.model.{Problem => _, _}
import it.pagopa.interop.selfcare.partymanagement.client.{model => PartyManagementDependency}
import it.pagopa.interop.selfcare.userregistry.client.model.UserResource
import it.pagopa.interop.tenantmanagement.client.{model => TenantManagementDependency}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

final case class ClientApiServiceImpl(
  authorizationManagementService: AuthorizationManagementService,
  agreementManagementService: AgreementManagementService,
  catalogManagementService: CatalogManagementService,
  partyManagementService: PartyManagementService,
  purposeManagementService: PurposeManagementService,
  userRegistryManagementService: UserRegistryManagementService,
  tenantManagementService: TenantManagementService
)(implicit ec: ExecutionContext)
    extends ClientApiService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  def internalServerError(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = complete((500, responseProblem))

  override def createConsumerClient(clientSeed: ClientSeed)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route = authorize(ADMIN_ROLE) {
    val operationLabel: String = s"Creating CONSUMER client with name ${clientSeed.name}"
    logger.info(operationLabel)

    val result = for {
      organizationId <- getOrganizationIdFutureUUID(contexts)
      _ = logger.info(s"Creating CONSUMER client ${clientSeed.name} for consumer $organizationId")
      client    <- authorizationManagementService.createClient(
        organizationId,
        clientSeed.name,
        clientSeed.description,
        authorizationmanagement.client.model.ClientKind.CONSUMER
      )(contexts)
      apiClient <- getClient(client)
    } yield apiClient

    onComplete(result) {
      handleConsumerClientCreationError(operationLabel) orElse { case Success(client) =>
        createConsumerClient201(client)
      }
    }
  }

  override def createApiClient(clientSeed: ClientSeed)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route = authorize(ADMIN_ROLE) {
    val operationLabel: String = s"Creating API client with name ${clientSeed.name}"
    logger.info(operationLabel)

    val result = for {
      organizationId <- getOrganizationIdFutureUUID(contexts)
      _ = logger.info(s"Creating API client ${clientSeed.name} for and consumer $organizationId")
      client    <- authorizationManagementService.createClient(
        organizationId,
        clientSeed.name,
        clientSeed.description,
        authorizationmanagement.client.model.ClientKind.API
      )(contexts)
      apiClient <- getClient(client)
    } yield apiClient

    onComplete(result) {
      handleApiClientCreationError(operationLabel) orElse { case Success(client) => createApiClient201(client) }
    }
  }

  override def getClient(clientId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, M2M_ROLE) {
    val operationLabel: String = s"Retrieving client $clientId"
    logger.info(operationLabel)

    val result: Future[Client] = for {
      clientUuid     <- clientId.toFutureUUID
      organizationId <- getOrganizationIdFutureUUID(contexts)
      client         <- authorizationManagementService.getClient(clientUuid)(contexts)
      apiClient      <- isConsumerOrProducer(organizationId, client).ifM(
        getClient(client),
        OrganizationNotAllowedOnClient(clientId, organizationId).raiseError[Future, Client]
      )
    } yield apiClient

    onComplete(result) {
      handleClientRetrieveError(operationLabel) orElse { case Success(client) => getClient200(client) }
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
    val operationLabel: String =
      s"Listing clients (offset: $offset and limit: $limit) for purposeId $purposeId and consumer $consumerId of kind $kind"
    logger.info(operationLabel)

    // TODO Improve multiple requests
    val result: Future[Seq[Client]] = for {
      personId      <- getUidFutureUUID(contexts)
      consumerUuid  <- consumerId.toFutureUUID
      clientKind    <- kind.traverse(AuthorizationManagementDependency.ClientKind.fromValue).toFuture
      selfcareId    <- tenantManagementService.getTenant(consumerUuid).flatMap(_.selfcareId.toFuture(MissingSelfcareId))
      relationships <-
        partyManagementService
          .getRelationships(
            selfcareId,
            personId,
            Seq(PartyManagementService.PRODUCT_ROLE_SECURITY_OPERATOR, PartyManagementService.PRODUCT_ROLE_ADMIN)
          )
          .map(_.items.filter(_.state == RelationshipState.ACTIVE))
      purposeUuid   <- purposeId.traverse(_.toFutureUUID)
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
      handleClientListingError(operationLabel) orElse { case Success(clients) => listClients200(Clients(clients)) }
    }
  }

  override def deleteClient(
    clientId: String
  )(implicit contexts: Seq[(String, String)], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    authorize(ADMIN_ROLE) {
      val operationLabel: String = s"Deleting client $clientId"
      logger.info(operationLabel)

      val result = for {
        clientUuid <- clientId.toFutureUUID
        _          <- assertIsConsumer(clientUuid)
        _          <- authorizationManagementService.deleteClient(clientUuid)(contexts)
      } yield ()

      onComplete(result) {
        handleClientDeleteError(operationLabel) orElse { case Success(_) => deleteClient204 }
      }
    }

  override def clientOperatorRelationshipBinding(clientId: String, relationshipId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route = authorize(ADMIN_ROLE) {
    val operationLabel: String = s"Binding client $clientId with relationship $relationshipId"
    logger.info(operationLabel)

    val result = for {
      clientUUID       <- clientId.toFutureUUID
      relationshipUUID <- relationshipId.toFutureUUID
      organizationId   <- getOrganizationIdFutureUUID(contexts)
      client           <- authorizationManagementService
        .getClient(clientUUID)(contexts)
        .ensureOr(client => OrganizationNotAllowedOnClient(clientId, client.consumerId))(_.consumerId == organizationId)
      relationship     <- getSecurityRelationship(relationshipUUID)
      updatedClient    <- client.relationships
        .find(_ === relationship.id)
        .fold(authorizationManagementService.addRelationship(clientUUID, relationship.id)(contexts))(_ =>
          Future.failed(OperatorRelationshipAlreadyAssigned(client.id, relationship.id))
        )
      apiClient        <- getClient(updatedClient)
    } yield apiClient

    onComplete(result) {
      handleClientOperatorRelationshipBindingError(operationLabel) orElse { case Success(client) =>
        clientOperatorRelationshipBinding201(client)
      }
    }
  }

  override def removeClientOperatorRelationship(clientId: String, relationshipId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE) {
    val operationLabel: String = s"Removing binding between $clientId with relationship $relationshipId"
    logger.info(operationLabel)

    val result = for {
      userUUID               <- getUidFutureUUID(contexts)
      clientUUID             <- clientId.toFutureUUID
      _                      <- assertIsConsumer(clientUUID)
      relationshipUUID       <- relationshipId.toFutureUUID
      requesterRelationships <- partyManagementService.getRelationshipsByPersonId(userUUID, Seq.empty)
      _                      <- Future
        .failed(UserNotAllowedToRemoveOwnRelationship(clientId, relationshipId))
        .whenA(isNotRemovable(relationshipUUID)(requesterRelationships))
      _ <- authorizationManagementService.removeClientRelationship(clientUUID, relationshipUUID)(contexts)
    } yield ()

    onComplete(result) {
      handleClientOperatorRelationshipDeleteError(operationLabel) orElse { case Success(_) =>
        removeClientOperatorRelationship204
      }
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
    val operationLabel: String = s"Getting Key $keyId of Client $clientId"
    logger.info(operationLabel)

    val result = for {
      clientUuid <- clientId.toFutureUUID
      _          <- assertIsConsumer(clientUuid)
      key        <- authorizationManagementService.getKey(clientUuid, keyId)
      operator   <- operatorFromRelationship(key.relationshipId)
    } yield AuthorizationManagementService.readKeyToApi(key, operator)

    onComplete(result) {
      handleClientKeyRetrieveError(operationLabel) orElse { case Success(readKey) => getClientKeyById200(readKey) }
    }
  }

  override def deleteClientKeyById(clientId: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE) {
    val operationLabel: String = s"Deleting Key $keyId of Client $clientId"
    logger.info(operationLabel)

    val result = for {
      clientUuid <- clientId.toFutureUUID
      _          <- assertIsConsumer(clientUuid)
      _          <- authorizationManagementService.deleteKey(clientUuid, keyId)(contexts)
    } yield ()

    onComplete(result) {
      handleClientKeyDeleteError(operationLabel) orElse { case Success(_) => deleteClientKeyById204 }
    }
  }

  override def createKeys(clientId: String, keysSeeds: Seq[KeySeed])(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE) {
    val operationLabel: String = s"Creating keys for client $clientId"
    logger.info(operationLabel)

    val result = for {
      userId         <- getUidFutureUUID(contexts)
      clientUuid     <- clientId.toFutureUUID
      organizationId <- getOrganizationIdFutureUUID(contexts)
      client         <- authorizationManagementService
        .getClient(clientUuid)(contexts)
        .ensureOr(client => OrganizationNotAllowedOnClient(clientId, client.consumerId))(_.consumerId == organizationId)
      relationshipId <- securityOperatorRelationship(client.consumerId, userId).map(_.id)
      seeds = keysSeeds.map(AuthorizationManagementService.toDependencyKeySeed(_, relationshipId))
      keysResponse <- authorizationManagementService.createKeys(clientUuid, seeds)(contexts)
    } yield ClientKeys(keysResponse.keys.map(AuthorizationManagementService.keyToApi))

    onComplete(result) {
      handleClientKeyCreationError(operationLabel) orElse { case Success(keys) => createKeys201(keys) }
    }
  }

  override def getClientKeys(clientId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientKeys: ToEntityMarshaller[ReadClientKeys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, M2M_ROLE) {
    val operationLabel: String = s"Retrieving keys for client $clientId"
    logger.info(operationLabel)

    val result = for {
      clientUuid   <- clientId.toFutureUUID
      _            <- assertIsConsumer(clientUuid)
      keysResponse <- authorizationManagementService.getClientKeys(clientUuid)(contexts)
      keys         <- Future.traverse(keysResponse.keys)(k =>
        operatorFromRelationship(k.relationshipId).map(operator =>
          AuthorizationManagementService.readKeyToApi(k, operator)
        )
      )
    } yield ReadClientKeys(keys)

    onComplete(result) {
      handleClientKeysRetrieveError(operationLabel) orElse { case Success(keys) => getClientKeys200(keys) }
    }
  }

  override def getClientOperators(clientId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerOperatorarray: ToEntityMarshaller[Seq[Operator]],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE) {
    val operationLabel: String = s"Retrieving operators of client $clientId"
    logger.info(operationLabel)

    val result = for {
      clientUuid     <- clientId.toFutureUUID
      organizationId <- getOrganizationIdFutureUUID(contexts)
      client         <- authorizationManagementService
        .getClient(clientUuid)(contexts)
        .ensureOr(client => OrganizationNotAllowedOnClient(clientId, client.consumerId))(_.consumerId == organizationId)
      operators      <- operatorsFromClient(client)
    } yield operators

    onComplete(result) {
      handleClientOperatorsRetrieveError(operationLabel) orElse { case Success(keys) => getClientOperators200(keys) }
    }
  }

  override def getClientOperatorRelationshipById(clientId: String, relationshipId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerOperator: ToEntityMarshaller[Operator],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, M2M_ROLE) {
    val operationLabel: String = s"Retrieving operator of client $clientId by relationship $relationshipId"
    logger.info(operationLabel)

    val result = for {
      clientUUID       <- clientId.toFutureUUID
      organizationId   <- getOrganizationIdFutureUUID(contexts)
      relationshipUUID <- relationshipId.toFutureUUID
      client           <- authorizationManagementService
        .getClient(clientUUID)(contexts)
        .ensureOr(client => OrganizationNotAllowedOnClient(clientId, client.consumerId))(_.consumerId == organizationId)
      _                <- hasClientRelationship(client, relationshipUUID)
      operator         <- operatorFromRelationship(relationshipUUID)
    } yield operator

    onComplete(result) {
      handleClientOperatorByRelationshipRetrieveError(operationLabel) orElse { case Success(operator) =>
        getClientOperatorRelationshipById200(operator)
      }
    }
  }

  override def addClientPurpose(clientId: String, details: PurposeAdditionDetails)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE) {
    val operationLabel: String = s"Adding Purpose ${details.purposeId} to Client $clientId"
    logger.info(operationLabel)

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
        .toFuture(AgreementNotFound(purpose.eserviceId, purpose.consumerId))
      descriptor <- eService.descriptors
        .find(_.id == agreement.descriptorId)
        .toFuture(DescriptorNotFound(purpose.eserviceId, agreement.descriptorId))
      version    <- purpose.versions
        .maxByOption(_.createdAt)
        .toFuture(PurposeNoVersionFound(purpose.id))
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
      handleClientPurposeAdditionError(operationLabel) orElse { case Success(_) => addClientPurpose204 }
    }
  }

  override def removeClientPurpose(clientId: String, purposeId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE) {
    val operationLabel: String = s"Removing Purpose $purposeId from Client $clientId"
    logger.info(operationLabel)

    val result: Future[Unit] = for {
      clientUuid  <- clientId.toFutureUUID
      _           <- assertIsConsumer(clientUuid)
      purposeUuid <- purposeId.toFutureUUID
      _           <- authorizationManagementService.removeClientPurpose(clientUuid, purposeUuid)(contexts)
    } yield ()

    onComplete(result) {
      handleClientPurposeRemoveError(operationLabel) orElse { case Success(_) => addClientPurpose204 }
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
          .toFuture(AgreementNotFound(eServiceId, client.consumerId))
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
        .toFuture(DescriptorNotFound(agreement.eserviceId, agreement.descriptorId))
    } yield (clientPurpose, purpose, agreement, eService, descriptor)

    for {
      consumer              <- tenantManagementService.getTenant(client.consumerId)
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
    selfcareId    <- tenantManagementService.getTenant(consumerId).flatMap(_.selfcareId.toFuture(MissingSelfcareId))
    relationships <- partyManagementService
      .getRelationships(
        selfcareId,
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
    consumer: TenantManagementDependency.Tenant,
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
      consumer = TenantManagementService.tenantToApi(consumer),
      name = client.name,
      purposes = purposesDetails.map(t => (purposeToApi _).tupled(t)),
      description = client.description,
      operators = Some(operator),
      kind = AuthorizationManagementService.convertToApiClientKind(client.kind)
    )
  }

  private[this] def assertIsConsumer(clientId: UUID)(implicit contexts: Seq[(String, String)]): Future[Unit] =
    for {
      organizationId <- getOrganizationIdFutureUUID(contexts)
      _              <- authorizationManagementService
        .getClient(clientId)(contexts)
        .ensureOr(client => OrganizationNotAllowedOnClient(clientId.toString, client.consumerId))(
          _.consumerId == organizationId
        )
    } yield ()

  override def getEncodedClientKeyById(clientId: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerEncodedClientKey: ToEntityMarshaller[EncodedClientKey],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, M2M_ROLE) {
    val operationLabel = s"Retrieving encoded Key $keyId of Client $clientId"
    logger.info(operationLabel)

    val result = for {
      clientUuid <- clientId.toFutureUUID
      _          <- assertIsConsumer(clientUuid)
      encodedKey <- authorizationManagementService.getEncodedClientKey(clientUuid, keyId)(contexts)
    } yield EncodedClientKey(key = encodedKey.key)

    onComplete(result) {
      handleEncodedKeyRetrieveError(operationLabel) orElse { case Success(key) => getEncodedClientKeyById200(key) }
    }
  }

}
