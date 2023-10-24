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
import it.pagopa.interop.authorizationprocess.service.model.{UserResource => CommonUserResource}
import it.pagopa.interop.authorizationprocess.service._
import it.pagopa.interop.authorizationprocess.service.SelfcareV2ClientService
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

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

final case class ClientApiServiceImpl(
  authorizationManagementService: AuthorizationManagementService,
  agreementManagementService: AgreementManagementService,
  catalogManagementService: CatalogManagementService,
  selfcareV2ClientService: SelfcareV2ClientService,
  purposeManagementService: PurposeManagementService,
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

  override def addUser(clientId: String, userId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route = authorize(ADMIN_ROLE) {
    val operationLabel: String = s"Binding client $clientId with user $userId"
    logger.info(operationLabel)

    val result: Future[Client] = for {
      clientUuid    <- clientId.toFutureUUID
      userUUID      <- userId.toFutureUUID
      requesterUUID <- getOrganizationIdFutureUUID(contexts)
      selfcareUUID  <- getSelfcareIdFutureUUID(contexts)
      client        <- authorizationManagementService
        .getClient(clientUuid)
        .ensureOr(client => OrganizationNotAllowedOnClient(clientId, client.consumerId))(_.consumerId == requesterUUID)
      userApi       <- getSecurityUser(selfcareUUID, requesterUUID, userUUID)
      updatedClient <- client.users
        .find(_ === userApi.id)
        .fold(authorizationManagementService.addUser(clientUuid, userUUID)(contexts))(_ =>
          Future.failed(UserAlreadyAssigned(client.id, userUUID))
        )
    } yield updatedClient.toApi(requesterUUID == updatedClient.consumerId)

    onComplete(result) {
      addUserResponse[Client](operationLabel)(addUser200)
    }
  }

  override def removeUser(clientId: String, userId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE) {
    val operationLabel: String = s"Removing binding between $clientId with user $userId"
    logger.info(operationLabel)

    val result: Future[Unit] = for {
      requesterUUID <- getUidFutureUUID(contexts)
      selfcareUUID  <- getSelfcareIdFutureUUID(contexts)
      clientUUID    <- clientId.toFutureUUID
      client        <- authorizationManagementService.getClient(clientUUID)
      _             <- assertIsClientConsumer(client).toFuture
      userUUID      <- userId.toFutureUUID
      users         <- selfcareV2ClientService
        .getInstitutionProductUsers(selfcareUUID, requesterUUID, userUUID.some, Seq.empty)
        .map(_.map(_.toApi))
      usersApi      <- users.traverse(_.toFuture)
      user          <- usersApi.headOption.toFuture(UserNotFound(selfcareUUID, userUUID))
      _             <-
        if (isNotRemovable(userUUID)(user)) Future.failed(UserNotAllowedToRemoveOwnUser(clientId, userId))
        else Future.unit
      _             <- authorizationManagementService.removeUser(clientUUID, userUUID)(contexts)
    } yield ()

    onComplete(result) {
      removeUserResponse[Unit](operationLabel)(_ => removeUser204)
    }
  }

  private def isNotRemovable(userId: UUID): CommonUserResource => Boolean = user => {
    user.id == userId &&
    user.roles.forall(_ != SelfcareV2ClientService.PRODUCT_ROLE_ADMIN)
  }

  override def getClientKeyById(clientId: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerKey: ToEntityMarshaller[Key],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, M2M_ROLE, SUPPORT_ROLE) {
    val operationLabel: String = s"Getting Key $keyId of Client $clientId"
    logger.info(operationLabel)

    val result: Future[Key] = for {
      clientUuid    <- clientId.toFutureUUID
      client        <- authorizationManagementService.getClient(clientUuid)
      _             <- assertIsClientConsumer(client).toFuture
      persistentKey <- authorizationManagementService.getClientKey(clientUuid, keyId)
      key           <- persistentKey.toApi.toFuture
    } yield key

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
      userUUID      <- getUidFutureUUID(contexts)
      clientUuid    <- clientId.toFutureUUID
      selfcareUUID  <- getSelfcareIdFutureUUID(contexts)
      requesterUUID <- getOrganizationIdFutureUUID(contexts)
      client        <- authorizationManagementService
        .getClient(clientUuid)
        .ensureOr(client => OrganizationNotAllowedOnClient(clientId, client.consumerId))(_.consumerId == requesterUUID)
      user          <- getSecurityUser(selfcareUUID, client.consumerId, userUUID)
      seeds = keysSeeds.map(_.toDependency(userUUID, dateTimeSupplier.get()))
      keysResponse <- authorizationManagementService.createKeys(clientUuid, seeds)(contexts)
    } yield Keys(keysResponse.keys.map(_.toApi))

    onComplete(result) {
      createKeysResponse[Keys](operationLabel)(createKeys200)
    }
  }

  override def getClientKeys(userIds: String, clientId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerKeys: ToEntityMarshaller[Keys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, M2M_ROLE, SUPPORT_ROLE) {
    val operationLabel: String = s"Retrieving keys for client $clientId"
    logger.info(operationLabel)

    val result: Future[Keys] = for {
      clientUuid <- clientId.toFutureUUID
      users      <- parseArrayParameters(userIds).traverse(_.toFutureUUID)
      client     <- authorizationManagementService.getClient(clientUuid)
      _          <- assertIsClientConsumer(client).toFuture
      clientKeys <- authorizationManagementService.getClientKeys(clientUuid)
      userKeys =
        if (users.isEmpty) clientKeys.map(_.toApi)
        else
          clientKeys.filter(key => key.userId.exists(users.contains)).map(_.toApi)
      keys <- userKeys.traverse(_.toFuture)
    } yield Keys(keys = keys)

    onComplete(result) {
      getClientKeysResponse[Keys](operationLabel)(getClientKeys200)
    }
  }

  override def getClientUsers(clientId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerUserarray: ToEntityMarshaller[Seq[User]]
  ): Route = authorize(ADMIN_ROLE, SECURITY_ROLE, SUPPORT_ROLE) {
    val operationLabel: String = s"Retrieving users of client $clientId"
    logger.info(operationLabel)

    val result: Future[Seq[User]] = for {
      clientUuid     <- clientId.toFutureUUID
      organizationId <- getOrganizationIdFutureUUID(contexts)
      client         <- authorizationManagementService
        .getClient(clientUuid)
        .ensureOr(client => OrganizationNotAllowedOnClient(clientId, client.consumerId))(_.consumerId == organizationId)
      users          <- usersFromClient(client)
    } yield users

    onComplete(result) {
      getClientUsersResponse[Seq[User]](operationLabel)(getClientUsers200)
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

  private[this] def getSecurityUser(
    selfcareId: UUID,
    requesterId: UUID,
    userId: UUID,
    roles: Seq[String] =
      Seq(SelfcareV2ClientService.PRODUCT_ROLE_SECURITY_USER, SelfcareV2ClientService.PRODUCT_ROLE_ADMIN)
  )(implicit contexts: Seq[(String, String)]): Future[CommonUserResource] = for {
    users    <- selfcareV2ClientService
      .getInstitutionProductUsers(selfcareId, requesterId, userId.some, roles)
      .map(_.map(_.toApi))
    usersApi <- users.traverse(_.toFuture)
    user     <- usersApi.headOption.toFuture(SecurityUserNotFound(requesterId, userId))
  } yield user

  private[this] def usersFromClient(client: PersistentClient)(implicit
    contexts: Seq[(String, String)]
  ): Future[Seq[User]] = Future.traverse(client.users.toList)(userFromUsers)

  private[this] def userFromUsers(userId: UUID)(implicit contexts: Seq[(String, String)]): Future[User] =
    for {
      idUUID            <- getUidFutureUUID(contexts)
      selfcareUUID      <- getSelfcareIdFutureUUID(contexts)
      users             <- selfcareV2ClientService
        .getInstitutionProductUsers(selfcareUUID, idUUID, userId.some, Seq.empty)
        .map(_.map(_.toApi))
      usersResourcesApi <- users.traverse(_.toFuture)
      userResourceApi   <- usersResourcesApi.headOption.toFuture(UserNotFound(selfcareUUID, userId))
      userResponse      <- selfcareV2ClientService.getUserById(selfcareUUID, userId).map(_.toApi)
      userResponseApi   <- userResponse.toFuture
    } yield User(
      userId = userResponseApi.id,
      taxCode = userResponseApi.taxCode,
      name = userResponseApi.name,
      familyName = userResponseApi.surname,
      roles = userResourceApi.roles
    )

  override def getClients(
    name: Option[String],
    userIds: String,
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
      s"Retrieving clients by name $name , userIds $userIds"
    logger.info(operationLabel)

    val result: Future[Clients] = for {
      requesterUuid <- getOrganizationIdFutureUUID(contexts)
      selfcareUuid  <- getSelfcareIdFutureUUID(contexts)
      userUuid      <- getUidFutureUUID(contexts)
      consumerUuid  <- consumerId.toFutureUUID
      purposeUUid   <- purposeId.traverse(_.toFutureUUID)
      roles         <- getUserRolesFuture(contexts)
      users         <- checkAuthorizationForRoles(roles, userIds, selfcareUuid, requesterUuid, userUuid)(contexts)
      clientKind    <- kind.traverse(ClientKind.fromValue).toFuture
      clients       <- authorizationManagementService.getClients(
        name,
        users,
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

  private def checkAuthorizationForRoles(
    roles: String,
    userIds: String,
    selfcareUuid: UUID,
    requesterId: UUID,
    userId: UUID
  )(implicit contexts: Seq[(String, String)]): Future[List[UUID]] = {
    if (roles.contains(SECURITY_ROLE)) getUser(selfcareUuid, requesterId, userId, Seq(SECURITY_ROLE)).map(List[UUID](_))
    else parseArrayParameters(userIds).traverse(_.toFutureUUID)
  }

  private def getUser(selfcareUuid: UUID, requesterId: UUID, userId: UUID, roles: Seq[String])(implicit
    contexts: Seq[(String, String)]
  ): Future[UUID] =
    getSecurityUser(selfcareUuid, requesterId, userId, roles).map(_.id)

  override def getClientsWithKeys(
    name: Option[String],
    userIds: String,
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
      s"Retrieving clients with keys by name $name , users $userIds"
    logger.info(operationLabel)

    val result: Future[ClientsWithKeys] = for {
      requesterUuid <- getOrganizationIdFutureUUID(contexts)
      selfcareUuid  <- getSelfcareIdFutureUUID(contexts)
      userUuid      <- getUidFutureUUID(contexts)
      consumerUuid  <- consumerId.toFutureUUID
      purposeUUid   <- purposeId.traverse(_.toFutureUUID)
      roles         <- getUserRolesFuture(contexts)
      users         <- checkAuthorizationForRoles(roles, userIds, selfcareUuid, requesterUuid, userUuid)(contexts)
      clientKind    <- kind.traverse(ClientKind.fromValue).toFuture
      clientsKeys   <- authorizationManagementService.getClientsWithKeys(
        name,
        users,
        consumerUuid,
        purposeUUid,
        clientKind.map(_.toProcess),
        offset,
        limit
      )
      apiClientsKeys = clientsKeys.results.map(_.toApi(requesterUuid == consumerUuid))
      keys <- apiClientsKeys.traverse(_.toFuture)
    } yield ClientsWithKeys(results = keys, totalCount = clientsKeys.totalCount)

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
