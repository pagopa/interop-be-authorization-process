package it.pagopa.interop.authorizationprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import cats.syntax.all._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop._
import it.pagopa.interop.agreementmanagement.model.agreement.{
  Active,
  PersistentAgreement,
  PersistentAgreementState,
  Suspended
}
import it.pagopa.interop.authorizationmanagement.client.{model => AuthorizationManagementDependency}
import it.pagopa.interop.authorizationmanagement.model.client.PersistentClient
import it.pagopa.interop.authorizationprocess.api.ClientApiService
import it.pagopa.interop.authorizationprocess.api.impl.ClientApiHandlers._
import it.pagopa.interop.authorizationprocess.common.Adapters._
import it.pagopa.interop.authorizationprocess.common.AuthorizationUtils._
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors._
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationprocess.service.PartyManagementService.{
  relationshipProductToApi,
  relationshipRoleToApi,
  relationshipStateToApi
}
import it.pagopa.interop.authorizationprocess.service._
import it.pagopa.interop.catalogmanagement.model.{CatalogDescriptor, Published, Deprecated => DeprecatedState}
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.commons.jwt._
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.AkkaUtils._
import it.pagopa.interop.commons.utils.OpenapiUtils.parseArrayParameters
import it.pagopa.interop.commons.utils.TypeConversions.{EitherOps, OptionOps, StringOps}
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import it.pagopa.interop.purposemanagement.model.purpose.{
  Archived,
  PersistentPurposeVersion,
  PersistentPurposeVersionState,
  Active => ActiveState
}
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
  dateTimeSupplier: OffsetDateTimeSupplier
)(implicit ec: ExecutionContext, readModel: ReadModelService)
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
        dateTimeSupplier.get(),
        clientSeed.members
      )(contexts)
    } yield client.toApi(organizationId == client.consumerId)

    onComplete(result) {
      createConsumerClientResponse[Client](operationLabel)(createConsumerClient200)
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
        dateTimeSupplier.get(),
        clientSeed.members
      )(contexts)
    } yield client.toApi(organizationId == client.consumerId)

    onComplete(result) {
      createApiClientResponse[Client](operationLabel)(createApiClient200)
    }
  }

  override def getClient(clientId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, M2M_ROLE, SUPPORT_ROLE) {
    val operationLabel: String = s"Retrieving client $clientId"
    logger.info(operationLabel)

    val result: Future[Client] = for {
      requesterUuid <- getOrganizationIdFutureUUID(contexts)
      clientUuid    <- clientId.toFutureUUID
      client        <- authorizationManagementService.getClient(clientUuid)
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
        client     <- authorizationManagementService.getClient(clientUuid)
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
      clientUuid       <- clientId.toFutureUUID
      relationshipUUID <- relationshipId.toFutureUUID
      organizationId   <- getOrganizationIdFutureUUID(contexts)
      client           <- authorizationManagementService
        .getClient(clientUuid)
        .ensureOr(client => OrganizationNotAllowedOnClient(clientId, client.consumerId))(_.consumerId == organizationId)
      relationship     <- getSecurityRelationship(relationshipUUID)
      updatedClient    <- client.relationships
        .find(_ === relationship.id)
        .fold(authorizationManagementService.addRelationship(clientUuid, relationship.id)(contexts))(_ =>
          Future.failed(OperatorRelationshipAlreadyAssigned(client.id, relationship.id))
        )
    } yield updatedClient.toApi(organizationId == updatedClient.consumerId)

    onComplete(result) {
      clientOperatorRelationshipBindingResponse[Client](operationLabel)(clientOperatorRelationshipBinding200)
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
      client                 <- authorizationManagementService.getClient(clientUUID)
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
    toEntityMarshallerKey: ToEntityMarshaller[Key],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, M2M_ROLE, SUPPORT_ROLE) {
    val operationLabel: String = s"Getting Key $keyId of Client $clientId"
    logger.info(operationLabel)

    val result: Future[Key] = for {
      clientUuid <- clientId.toFutureUUID
      client     <- authorizationManagementService.getClient(clientUuid)
      _          <- assertIsClientConsumer(client).toFuture
      key        <- authorizationManagementService.getClientKey(clientUuid, keyId)
    } yield key.toApi

    onComplete(result) {
      getClientKeyByIdResponse[Key](operationLabel)(getClientKeyById200)
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
      client     <- authorizationManagementService.getClient(clientUuid)
      _          <- assertIsClientConsumer(client).toFuture
      _          <- authorizationManagementService.deleteKey(clientUuid, keyId)(contexts)
    } yield ()

    onComplete(result) {
      deleteClientKeyByIdResponse[Unit](operationLabel)(_ => deleteClientKeyById204)
    }
  }

  override def createKeys(clientId: String, keysSeeds: Seq[KeySeed])(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerKeys: ToEntityMarshaller[Keys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE) {
    val operationLabel: String = s"Creating keys for client $clientId"
    logger.info(operationLabel)

    val result: Future[Keys] = for {
      userId         <- getUidFutureUUID(contexts)
      clientUuid     <- clientId.toFutureUUID
      organizationId <- getOrganizationIdFutureUUID(contexts)
      client         <- authorizationManagementService
        .getClient(clientUuid)
        .ensureOr(client => OrganizationNotAllowedOnClient(clientId, client.consumerId))(_.consumerId == organizationId)
      relationshipId <- securityOperatorRelationship(client.consumerId, userId).map(_.id)
      seeds = keysSeeds.map(_.toDependency(relationshipId, dateTimeSupplier.get()))
      keysResponse <- authorizationManagementService.createKeys(clientUuid, seeds)(contexts)
    } yield Keys(keysResponse.keys.map(_.toApi))

    onComplete(result) {
      createKeysResponse[Keys](operationLabel)(createKeys200)
    }
  }

  override def getClientKeys(relationshipIds: String, clientId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerKeys: ToEntityMarshaller[Keys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, M2M_ROLE, SUPPORT_ROLE) {
    val operationLabel: String = s"Retrieving keys for client $clientId"
    logger.info(operationLabel)

    val result: Future[Keys] = for {
      clientUuid    <- clientId.toFutureUUID
      relationships <- parseArrayParameters(relationshipIds).traverse(_.toFutureUUID)
      client        <- authorizationManagementService.getClient(clientUuid)
      _             <- assertIsClientConsumer(client).toFuture
      clientKeys    <- authorizationManagementService.getClientKeys(clientUuid)
      operatorKeys =
        if (relationships.isEmpty) clientKeys
        else
          clientKeys.filter(key => relationships.contains(key.relationshipId))
    } yield Keys(operatorKeys.map(_.toApi))

    onComplete(result) {
      getClientKeysResponse[Keys](operationLabel)(getClientKeys200)
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
        .getClient(clientUuid)
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

    val validAgreementsStates: Set[PersistentAgreementState] =
      Set[PersistentAgreementState](Active, Suspended)

    val invalidPurposeStates: Set[PersistentPurposeVersionState] =
      Set(Archived)

    def descriptorToComponentState(
      descriptor: CatalogDescriptor
    ): AuthorizationManagementDependency.ClientComponentState =
      descriptor.state match {
        case Published | DeprecatedState =>
          AuthorizationManagementDependency.ClientComponentState.ACTIVE
        case _                           => AuthorizationManagementDependency.ClientComponentState.INACTIVE
      }

    def agreementToComponentState(
      agreement: PersistentAgreement
    ): AuthorizationManagementDependency.ClientComponentState =
      agreement.state match {
        case Active =>
          AuthorizationManagementDependency.ClientComponentState.ACTIVE
        case _      => AuthorizationManagementDependency.ClientComponentState.INACTIVE
      }

    def purposeVersionToComponentState(
      purposeVersion: PersistentPurposeVersion
    ): AuthorizationManagementDependency.ClientComponentState =
      purposeVersion.state match {
        case ActiveState =>
          AuthorizationManagementDependency.ClientComponentState.ACTIVE
        case _           => AuthorizationManagementDependency.ClientComponentState.INACTIVE
      }

    val result: Future[Unit] = for {
      clientUuid <- clientId.toFutureUUID
      client     <- authorizationManagementService.getClient(clientUuid)
      _          <- assertIsClientConsumer(client).toFuture
      purpose    <- purposeManagementService.getPurposeById(details.purposeId)
      _          <- assertIsPurposeConsumer(purpose.id, purpose.consumerId).toFuture
      eService   <- catalogManagementService.getEServiceById(purpose.eserviceId)
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
      client      <- authorizationManagementService.getClient(clientUuid)
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
    selfcareId <- tenantManagementService.getTenantById(requesterId).flatMap(_.selfcareId.toFuture(MissingSelfcareId))
    relationships <- partyManagementService
      .getRelationships(selfcareId, userId, roles)
    activeRelationShips = relationships.items.toList.filter(
      _.state == PartyManagementDependency.RelationshipState.ACTIVE
    )
    relationShip <- activeRelationShips.headOption.toFuture(SecurityOperatorRelationshipNotFound(requesterId, userId))
  } yield relationShip

  private[this] def operatorsFromClient(client: PersistentClient)(implicit
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
      clients       <- authorizationManagementService.getClients(
        name,
        relationships,
        consumerUuid,
        purposeUUid,
        clientKind.map(_.toProcess),
        offset,
        limit
      )
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
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, M2M_ROLE, SUPPORT_ROLE) {

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
      clientsKeys   <- authorizationManagementService.getClientsWithKeys(
        name,
        relationships,
        consumerUuid,
        purposeUUid,
        clientKind.map(_.toProcess),
        offset,
        limit
      )
      apiClientsKeys = clientsKeys.results.map(_.toApi(requesterUuid == consumerUuid))
    } yield ClientsWithKeys(results = apiClientsKeys, totalCount = clientsKeys.totalCount)

    onComplete(result) { getClientsWithKeysResponse[ClientsWithKeys](operationLabel)(getClientsWithKeys200) }

  }

  override def removePurposeFromClients(
    purposeId: String
  )(implicit contexts: Seq[(String, String)], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    authorize(ADMIN_ROLE) {
      val operationLabel: String = s"Removing Purpose $purposeId from all clients"
      logger.info(operationLabel)

      val result: Future[Unit] = for {
        purposeUuid <- purposeId.toFutureUUID
        clients     <- authorizationManagementService.getClientsByPurpose(purposeUuid)
        _           <- Future.traverse(clients)(c =>
          authorizationManagementService.removeClientPurpose(c.id, purposeUuid)(contexts)
        )
      } yield ()

      onComplete(result) {
        removePurposeFromClientsResponse[Unit](operationLabel)(_ => removePurposeFromClients204)
      }
    }
}
