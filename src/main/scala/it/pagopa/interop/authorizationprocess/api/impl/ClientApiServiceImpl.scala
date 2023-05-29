package it.pagopa.interop.authorizationprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import cats.syntax.all._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop._
import it.pagopa.interop.agreementmanagement.client.{model => AgreementManagementDependency}
import it.pagopa.interop.authorizationmanagement.client.{model => AuthorizationManagementDependency}
import it.pagopa.interop.authorizationprocess.api.ClientApiService
import it.pagopa.interop.authorizationprocess.api.impl.ClientApiHandlers._
import it.pagopa.interop.authorizationprocess.common.Adapters._
import it.pagopa.interop.authorizationprocess.common.AuthorizationUtils._
import it.pagopa.interop.authorizationprocess.common.readmodel.ReadModelQueries
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors._
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationprocess.service.PartyManagementService.{relationshipProductToApi, relationshipRoleToApi, relationshipStateToApi}
import it.pagopa.interop.authorizationprocess.service._
import it.pagopa.interop.catalogmanagement.client.{model => CatalogManagementDependency}
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.commons.jwt.{ADMIN_ROLE, M2M_ROLE, SECURITY_ROLE, SUPPORT_ROLE, authorize}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.AkkaUtils._
import it.pagopa.interop.commons.utils.OpenapiUtils.parseArrayParameters
import it.pagopa.interop.commons.utils.TypeConversions.{EitherOps, OptionOps, StringOps}
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import it.pagopa.interop.purposemanagement.client.{model => PurposeManagementDependency}
import it.pagopa.interop.selfcare._
import it.pagopa.interop.selfcare.partymanagement.client.model.{Problem => _, _}
import it.pagopa.interop.selfcare.partymanagement.client.{model => PartyManagementDependency}
import it.pagopa.interop.selfcare.userregistry.client.model.UserResource

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

final case class ClientApiServiceImpl(
  authorizationManagementService: AuthorizationManagementService,
  agreementManagementService: AgreementManagementService,
  catalogManagementService: CatalogManagementService,
  partyManagementService: PartyManagementService,
  purposeManagementService: PurposeManagementService,
  userRegistryManagementService: UserRegistryManagementService,
  tenantManagementService: TenantManagementService,
  readModel: ReadModelService,
  dateTimeSupplier: OffsetDateTimeSupplier
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

    val result: Future[Client] = for {
      organizationId <- getOrganizationIdFutureUUID(contexts)
      _ = logger.info(s"Creating CONSUMER client ${clientSeed.name} for consumer $organizationId")
      client <- authorizationManagementService.createClient(
        organizationId,
        clientSeed.name,
        clientSeed.description,
        authorizationmanagement.client.model.ClientKind.CONSUMER,
        dateTimeSupplier.get()
      )(contexts)
    } yield client.toApi(organizationId == client.consumerId)

    onComplete(result) {
      createConsumerClientResponse[Client](operationLabel)(createConsumerClient201)
    }
  }

  override def createApiClient(clientSeed: ClientSeed)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route = authorize(ADMIN_ROLE) {
    val operationLabel: String = s"Creating API client with name ${clientSeed.name}"
    logger.info(operationLabel)

    val result: Future[Client] = for {
      organizationId <- getOrganizationIdFutureUUID(contexts)
      _ = logger.info(s"Creating API client ${clientSeed.name} for and consumer $organizationId")
      client <- authorizationManagementService.createClient(
        organizationId,
        clientSeed.name,
        clientSeed.description,
        authorizationmanagement.client.model.ClientKind.API,
        dateTimeSupplier.get()
      )(contexts)
    } yield client.toApi(organizationId == client.consumerId)

    onComplete(result) {
      createApiClientResponse[Client](operationLabel)(createApiClient201)
    }
  }

  override def getClient(clientId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, M2M_ROLE, SUPPORT_ROLE) {
    val operationLabel: String = s"Retrieving client $clientId"
    logger.info(operationLabel)

    def isConsumerOrProducer(organizationId: UUID, client: ManagementClient)(implicit
      ec: ExecutionContext,
      contexts: Seq[(String, String)]
    ): Future[Boolean] = {
      def isProducer(): Future[Boolean] = Future
        .traverse(client.purposes.map(_.states.eservice.eserviceId))(catalogManagementService.getEService)
        .map(eservices => eservices.map(_.producerId).contains(organizationId))

      if (client.consumerId == organizationId) Future.successful(true) else isProducer()

    }

    val result: Future[Client] = for {
      requesterUuid        <- getOrganizationIdFutureUUID(contexts)
      clientUuid           <- clientId.toFutureUUID
      client               <- authorizationManagementService.getClient(clientUuid)(contexts)
      isConsumerOrProducer <- isConsumerOrProducer(requesterUuid, client)
      _                    <- Future
        .failed(OrganizationNotAllowedOnClient(client.id.toString, requesterUuid))
        .unlessA(isConsumerOrProducer)

    } yield client.toApi(requesterUuid == client.consumerId)

    onComplete(result) {
      getClientResponse[Client](operationLabel)(getClient200)
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
        client     <- authorizationManagementService.getClient(clientUuid)(contexts)
        _          <- assertIsClientConsumer(client).toFuture
        _          <- authorizationManagementService.deleteClient(clientUuid)(contexts)
      } yield ()

      onComplete(result) {
        deleteClientResponse[Unit](operationLabel)(_ => deleteClient204)
      }
    }

  override def clientOperatorRelationshipBinding(clientId: String, relationshipId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route = authorize(ADMIN_ROLE) {
    val operationLabel: String = s"Binding client $clientId with relationship $relationshipId"
    logger.info(operationLabel)

    val result: Future[Client] = for {
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
    } yield updatedClient.toApi(organizationId == updatedClient.consumerId)

    onComplete(result) {
      clientOperatorRelationshipBindingResponse[Client](operationLabel)(clientOperatorRelationshipBinding201)
    }
  }

  override def removeClientOperatorRelationship(clientId: String, relationshipId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE) {
    val operationLabel: String = s"Removing binding between $clientId with relationship $relationshipId"
    logger.info(operationLabel)

    val result: Future[Unit] = for {
      userUUID               <- getUidFutureUUID(contexts)
      clientUUID             <- clientId.toFutureUUID
      client                 <- authorizationManagementService.getClient(clientUUID)(contexts)
      _                      <- assertIsClientConsumer(client).toFuture
      relationshipUUID       <- relationshipId.toFutureUUID
      requesterRelationships <- partyManagementService.getRelationshipsByPersonId(userUUID, Seq.empty)
      _                      <- Future
        .failed(UserNotAllowedToRemoveOwnRelationship(clientId, relationshipId))
        .whenA(isNotRemovable(relationshipUUID)(requesterRelationships))
      _ <- authorizationManagementService.removeClientRelationship(clientUUID, relationshipUUID)(contexts)
    } yield ()

    onComplete(result) {
      removeClientOperatorRelationshipResponse[Unit](operationLabel)(_ => removeClientOperatorRelationship204)
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
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, M2M_ROLE, SUPPORT_ROLE) {
    val operationLabel: String = s"Getting Key $keyId of Client $clientId"
    logger.info(operationLabel)

    val result: Future[ReadClientKey] = for {
      clientUuid <- clientId.toFutureUUID
      client     <- authorizationManagementService.getClient(clientUuid)(contexts)
      _          <- assertIsClientConsumer(client).toFuture
      key        <- authorizationManagementService.getKey(clientUuid, keyId)
      operator   <- operatorFromRelationship(key.relationshipId)
    } yield key.toReadKeyApi(operator)

    onComplete(result) {
      getClientKeyByIdResponse[ReadClientKey](operationLabel)(getClientKeyById200)
    }
  }

  override def deleteClientKeyById(clientId: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE) {
    val operationLabel: String = s"Deleting Key $keyId of Client $clientId"
    logger.info(operationLabel)

    val result: Future[Unit] = for {
      clientUuid <- clientId.toFutureUUID
      client     <- authorizationManagementService.getClient(clientUuid)(contexts)
      _          <- assertIsClientConsumer(client).toFuture
      _          <- authorizationManagementService.deleteKey(clientUuid, keyId)(contexts)
    } yield ()

    onComplete(result) {
      deleteClientKeyByIdResponse[Unit](operationLabel)(_ => deleteClientKeyById204)
    }
  }

  override def createKeys(clientId: String, keysSeeds: Seq[KeySeed])(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE) {
    val operationLabel: String = s"Creating keys for client $clientId"
    logger.info(operationLabel)

    val result: Future[ClientKeys] = for {
      userId         <- getUidFutureUUID(contexts)
      clientUuid     <- clientId.toFutureUUID
      organizationId <- getOrganizationIdFutureUUID(contexts)
      client         <- authorizationManagementService
        .getClient(clientUuid)(contexts)
        .ensureOr(client => OrganizationNotAllowedOnClient(clientId, client.consumerId))(_.consumerId == organizationId)
      relationshipId <- securityOperatorRelationship(client.consumerId, userId).map(_.id)
      seeds = keysSeeds.map(_.toDependency(relationshipId, dateTimeSupplier.get()))
      keysResponse <- authorizationManagementService.createKeys(clientUuid, seeds)(contexts)
    } yield ClientKeys(keysResponse.keys.map(_.toApi))

    onComplete(result) {
      createKeysResponse[ClientKeys](operationLabel)(createKeys201)
    }
  }

  override def getClientKeys(relationshipIds: String, clientId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientKeys: ToEntityMarshaller[ReadClientKeys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, M2M_ROLE, SUPPORT_ROLE) {
    val operationLabel: String = s"Retrieving keys for client $clientId"
    logger.info(operationLabel)

    val result: Future[ReadClientKeys] = for {
      clientUuid    <- clientId.toFutureUUID
      relationships <- parseArrayParameters(relationshipIds).traverse(_.toFutureUUID)
      client        <- authorizationManagementService.getClient(clientUuid)(contexts)
      _             <- assertIsClientConsumer(client).toFuture
      clientKeys    <- authorizationManagementService.getClientKeys(clientUuid)(contexts)
      operatorKeys =
        if (relationships.isEmpty) clientKeys.keys
        else
          clientKeys.keys.filter(key => relationships.contains(key.relationshipId))
      keysResponse = authorizationmanagement.client.model.KeysResponse(operatorKeys)
      keys <- Future.traverse(keysResponse.keys)(k => operatorFromRelationship(k.relationshipId).map(k.toReadKeyApi))
    } yield ReadClientKeys(keys)

    onComplete(result) {
      getClientKeysResponse[ReadClientKeys](operationLabel)(getClientKeys200)
    }
  }

  override def getClientOperators(clientId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerOperatorarray: ToEntityMarshaller[Seq[Operator]],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, SUPPORT_ROLE) {
    val operationLabel: String = s"Retrieving operators of client $clientId"
    logger.info(operationLabel)

    val result: Future[Seq[Operator]] = for {
      clientUuid     <- clientId.toFutureUUID
      organizationId <- getOrganizationIdFutureUUID(contexts)
      client         <- authorizationManagementService
        .getClient(clientUuid)(contexts)
        .ensureOr(client => OrganizationNotAllowedOnClient(clientId, client.consumerId))(_.consumerId == organizationId)
      operators      <- operatorsFromClient(client)
    } yield operators

    onComplete(result) {
      getClientOperatorsResponse[Seq[Operator]](operationLabel)(getClientOperators200)
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

    val invalidPurposeStates: Set[PurposeManagementDependency.PurposeVersionState] =
      Set(PurposeManagementDependency.PurposeVersionState.ARCHIVED)

    def descriptorToComponentState(
      descriptor: CatalogManagementDependency.EServiceDescriptor
    ): AuthorizationManagementDependency.ClientComponentState = descriptor.state match {
      case CatalogManagementDependency.EServiceDescriptorState.PUBLISHED |
          CatalogManagementDependency.EServiceDescriptorState.DEPRECATED =>
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
      client     <- authorizationManagementService.getClient(clientUuid)(contexts)
      _          <- assertIsClientConsumer(client).toFuture
      purpose    <- purposeManagementService.getPurpose(details.purposeId)
      _          <- assertIsPurposeConsumer(purpose.id, purpose.consumerId).toFuture
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
        .filterNot(v => invalidPurposeStates(v.state))
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
      addClientPurposeResponse[Unit](operationLabel)(_ => addClientPurpose204)
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
      client      <- authorizationManagementService.getClient(clientUuid)(contexts)
      _           <- assertIsClientConsumer(client).toFuture
      purposeUuid <- purposeId.toFutureUUID
      _           <- authorizationManagementService.removeClientPurpose(clientUuid, purposeUuid)(contexts)
    } yield ()

    onComplete(result) {
      removeClientPurposeResponse[Unit](operationLabel)(_ => addClientPurpose204)
    }
  }

  private[this] def getSecurityRelationship(
    relationshipId: UUID
  )(implicit contexts: Seq[(String, String)]): Future[partymanagement.client.model.Relationship] = {

    def isActiveSecurityOperatorRelationship(relationship: Relationship): Future[Boolean] = {

      val isValidProductRole: Boolean =
        Set(PartyManagementService.PRODUCT_ROLE_SECURITY_OPERATOR, PartyManagementService.PRODUCT_ROLE_ADMIN)
          .contains(relationship.product.role)

      val isActive: Boolean = relationship.state == PartyManagementDependency.RelationshipState.ACTIVE

      val condition: Boolean = isValidProductRole && isActive

      if (condition) Future.successful(true)
      else Future.failed(SecurityOperatorRelationshipNotActive(relationshipId))
    }

    for {
      relationship <- partyManagementService.getRelationshipById(relationshipId)
      _            <- isActiveSecurityOperatorRelationship(relationship)
    } yield relationship
  }

  private[this] def securityOperatorRelationship(
    requesterId: UUID,
    userId: UUID,
    roles: Seq[String] =
      Seq(PartyManagementService.PRODUCT_ROLE_SECURITY_OPERATOR, PartyManagementService.PRODUCT_ROLE_ADMIN)
  )(implicit contexts: Seq[(String, String)]): Future[PartyManagementDependency.Relationship] = for {
    selfcareId    <- tenantManagementService.getTenant(requesterId).flatMap(_.selfcareId.toFuture(MissingSelfcareId))
    relationships <- partyManagementService
      .getRelationships(selfcareId, userId, roles)
    activeRelationShips = relationships.items.toList.filter(
      _.state == PartyManagementDependency.RelationshipState.ACTIVE
    )
    relationShip <- activeRelationShips.headOption.toFuture(SecurityOperatorRelationshipNotFound(requesterId, userId))
  } yield relationShip

  private[this] def operatorsFromClient(client: AuthorizationManagementDependency.Client)(implicit
    contexts: Seq[(String, String)]
  ): Future[Seq[Operator]] = Future.traverse(client.relationships.toList)(operatorFromRelationship)

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

  override def getEncodedClientKeyById(clientId: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerEncodedClientKey: ToEntityMarshaller[EncodedClientKey],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, M2M_ROLE, SUPPORT_ROLE) {
    val operationLabel = s"Retrieving encoded Key $keyId of Client $clientId"
    logger.info(operationLabel)

    val result: Future[EncodedClientKey] = for {
      clientUuid <- clientId.toFutureUUID
      client     <- authorizationManagementService.getClient(clientUuid)(contexts)
      _          <- assertIsClientConsumer(client).toFuture
      encodedKey <- authorizationManagementService.getEncodedClientKey(clientUuid, keyId)(contexts)
    } yield EncodedClientKey(key = encodedKey.key)

    onComplete(result) {
      getEncodedClientKeyByIdResponse[EncodedClientKey](operationLabel)(getEncodedClientKeyById200)
    }
  }

  override def getClients(
    name: Option[String],
    relationshipIds: String,
    consumerId: String,
    purposeId: Option[String],
    kind: Option[String],
    offset: Int,
    limit: Int
  )(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClients: ToEntityMarshaller[Clients],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, M2M_ROLE, SUPPORT_ROLE) {
    val operationLabel =
      s"Retrieving clients by name $name , relationship $relationshipIds"
    logger.info(operationLabel)

    val result: Future[Clients] = for {
      requesterUuid <- getOrganizationIdFutureUUID(contexts)
      userUuid      <- getUidFutureUUID(contexts)
      consumerUuid  <- consumerId.toFutureUUID
      purposeUUid   <- purposeId.traverse(_.toFutureUUID)
      roles         <- getUserRolesFuture(contexts)
      relationships <- checkAuthorizationForRoles(roles, relationshipIds, requesterUuid, userUuid)(contexts)
      clientKind    <- kind.traverse(ClientKind.fromValue).toFuture
      clients       <- ReadModelQueries.listClients(
        name,
        relationships,
        consumerUuid,
        purposeUUid,
        clientKind.map(_.toProcess),
        offset,
        limit
      )(readModel)
      apiClients = clients.results.map(_.toApi(requesterUuid == consumerUuid))
    } yield Clients(results = apiClients, totalCount = clients.totalCount)

    onComplete(result) { getClientsResponse[Clients](operationLabel)(getClients200) }

  }

  private def checkAuthorizationForRoles(roles: String, relationshipIds: String, requesterId: UUID, userId: UUID)(
    implicit contexts: Seq[(String, String)]
  ): Future[List[UUID]] = {
    if (roles.contains(SECURITY_ROLE)) getRelationship(userId, requesterId, Seq(SECURITY_ROLE)).map(List[UUID](_))
    else parseArrayParameters(relationshipIds).traverse(_.toFutureUUID)
  }

  private def getRelationship(userId: UUID, requesterId: UUID, roles: Seq[String])(implicit
    contexts: Seq[(String, String)]
  ): Future[UUID] =
    securityOperatorRelationship(requesterId, userId, roles).map(_.id)

  override def getClientsWithKeys(
    name: Option[String],
    relationshipIds: String,
    consumerId: String,
    purposeId: Option[String],
    kind: Option[String],
    offset: Int,
    limit: Int
  )(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientsWithKeys: ToEntityMarshaller[ClientsWithKeys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, M2M_ROLE,SUPPORT_ROLE) {

    val operationLabel =
      s"Retrieving clients with keys by name $name , relationship $relationshipIds"
    logger.info(operationLabel)

    val result: Future[ClientsWithKeys] = for {
      requesterUuid <- getOrganizationIdFutureUUID(contexts)
      userUuid      <- getUidFutureUUID(contexts)
      consumerUuid  <- consumerId.toFutureUUID
      purposeUUid   <- purposeId.traverse(_.toFutureUUID)
      roles         <- getUserRolesFuture(contexts)
      relationships <- checkAuthorizationForRoles(roles, relationshipIds, requesterUuid, userUuid)(contexts)
      clientKind    <- kind.traverse(ClientKind.fromValue).toFuture
      clientsKeys   <- ReadModelQueries.listClientsWithKeys(
        name,
        relationships,
        consumerUuid,
        purposeUUid,
        clientKind.map(_.toProcess),
        offset,
        limit
      )(readModel)
      apiClientsKeys = clientsKeys.results.map(_.toApi(requesterUuid == consumerUuid))
    } yield ClientsWithKeys(results = apiClientsKeys, totalCount = clientsKeys.totalCount)

    onComplete(result) { getClientsWithKeysResponse[ClientsWithKeys](operationLabel)(getClientsWithKeys200) }

  }

  override def removeArchivedPurpose(
    purposeId: String
  )(implicit contexts: Seq[(String, String)], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    authorize(ADMIN_ROLE) {
      val operationLabel: String = s"Removing archived Purpose $purposeId from all clients"
      logger.info(operationLabel)

      val result: Future[Unit] = for {
        purposeUuid <- purposeId.toFutureUUID
        purpose     <- purposeManagementService.getPurpose(purposeUuid)
        _           <- purpose.versions
          .maxByOption(_.createdAt)
          .find(_.state == PurposeManagementDependency.PurposeVersionState.ARCHIVED)
          .toFuture(PurposeNotInExpectedState(purpose.id))
        clients     <- ReadModelQueries.listClientsByPurpose(purposeUuid)(readModel)
        _           <- Future.traverse(clients)(c =>
          authorizationManagementService.removeClientPurpose(c.id, purposeUuid)(contexts)
        )
      } yield ()

      onComplete(result) {
        removeArchivedPurposeResponse[Unit](operationLabel)(_ => removeArchivedPurpose204)
      }
    }
}
