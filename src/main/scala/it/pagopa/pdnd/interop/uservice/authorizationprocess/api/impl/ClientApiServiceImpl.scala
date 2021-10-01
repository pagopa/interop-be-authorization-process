package it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import cats.implicits._
import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.model.AgreementEnums
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.ClientApiService
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.utils.{EitherOps, OptionOps, toUuid}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.error._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.client.invoker.{ApiError => CatalogManagementApiError}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.client.model.EServiceDescriptor
import it.pagopa.pdnd.interop.uservice.keymanagement.client.invoker.{ApiError => AuthorizationManagementApiError}
import it.pagopa.pdnd.interop.uservice.partymanagement.client.model.{Problem => _, _}
import it.pagopa.pdnd.interop.uservice.{agreementmanagement, catalogmanagement, keymanagement, partymanagement}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.Product"))
final case class ClientApiServiceImpl(
  authorizationManagementService: AuthorizationManagementService,
  agreementManagementService: AgreementManagementService,
  catalogManagementService: CatalogManagementService,
  partyManagementService: PartyManagementService
)(implicit ec: ExecutionContext)
    extends ClientApiService {

  /** Code: 201, Message: Client created, DataType: Client
    * Code: 401, Message: Unauthorized, DataType: Problem
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
      _           <- catalogManagementService.getEService(bearerToken, clientSeed.eServiceId.toString)
      consumer    <- partyManagementService.getOrganizationByInstitutionId(clientSeed.consumerInstitutionId)
      consumerId  <- toUuid(consumer.partyId).toFuture
      client <- authorizationManagementService.createClient(
        clientSeed.eServiceId,
        consumerId,
        clientSeed.name,
        clientSeed.purposes,
        clientSeed.description
      )
      apiClient <- getClient(bearerToken, client)
    } yield apiClient

    onComplete(result) {
      case Success(client)                    => createClient201(client)
      case Failure(ex @ UnauthenticatedError) => createClient401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: CatalogManagementApiError[_]) if ex.code == 404 =>
        createClient404(Problem(Some(s"E-Service id ${clientSeed.eServiceId.toString} not found"), 404, "Not found"))
      case Failure(ex) => createClient500(Problem(Option(ex.getMessage), 500, "Error on client creation"))
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
    val result = for {
      bearerToken <- extractBearer(contexts)
      client      <- authorizationManagementService.getClient(clientId)
      apiClient   <- getClient(bearerToken, client)
    } yield apiClient

    onComplete(result) {
      case Success(client)                    => getClient200(client)
      case Failure(ex @ UnauthenticatedError) => getClient401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        getClient404(Problem(Some(ex.message), 404, "Client not found"))
      case Failure(ex) => getClient500(Problem(Option(ex.getMessage), 500, "Error on client retrieve"))
    }
  }

  /** Code: 200, Message: Request succeed, DataType: Seq[Client]
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 500, Message: Internal Server Error, DataType: Problem
    */
  override def listClients(
    offset: Option[Int],
    limit: Option[Int],
    eServiceId: Option[String],
    operatorTaxCode: Option[String],
    institutionId: Option[String]
  )(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClientarray: ToEntityMarshaller[Seq[Client]]
  ): Route = {
    // TODO Improve multiple requests
    val result = for {
      bearerToken  <- extractBearer(contexts)
      eServiceUuid <- eServiceId.traverse(toUuid).toFuture
      relationships <- operatorTaxCode.traverse(
        partyManagementService.getRelationshipsByTaxCode(_, Some(PartyManagementService.ROLE_SECURITY_OPERATOR))
      )
      consumer   <- institutionId.traverse(partyManagementService.getOrganizationByInstitutionId)
      consumerId <- consumer.traverse(c => toUuid(c.partyId)).toFuture
      clients <- relationships match {
        case None => authorizationManagementService.listClients(offset, limit, eServiceUuid, None, consumerId)
        case Some(rels) =>
          rels.items.flatTraverse(rel =>
            authorizationManagementService.listClients(offset, limit, eServiceUuid, Some(rel.id), consumerId)
          )
      }
      clientsDetails <- clients.traverse(client => getClient(bearerToken, client))
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
      bearerToken  <- extractBearer(contexts)
      clientUuid   <- toUuid(clientId).toFuture
      client       <- authorizationManagementService.getClient(clientId)
      relationship <- getOrCreateRelationship(client, operatorSeed)
      updatedClient <- client.relationships
        .find(_ === relationship.id)
        .fold(authorizationManagementService.addRelationship(clientUuid, relationship.id))(_ =>
          Future.failed(SecurityOperatorAlreadyAssigned(client.id.toString, operatorSeed.taxCode))
        )
      apiClient <- getClient(bearerToken, updatedClient)
    } yield apiClient

    onComplete(result) {
      case Success(client)                    => addOperator201(client)
      case Failure(ex @ UnauthenticatedError) => addOperator401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: UuidConversionError)   => addOperator400(Problem(Option(ex.getMessage), 400, "Bad request"))
      case Failure(ex: SecurityOperatorAlreadyAssigned) =>
        addOperator400(Problem(Option(ex.getMessage), 400, "Bad request"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        addOperator404(Problem(Some(ex.message), 404, "Client not found"))
      case Failure(ex) => addOperator500(Problem(Option(ex.getMessage), 500, "Error on operator addition"))
    }
  }

  private[this] def recoverIfMissing[T](toRecover: Future[T], recoverWith: => Future[T]): Future[T] =
    toRecover.recoverWith {
      case ex: partymanagement.client.invoker.ApiError[_] if ex.code == 404 =>
        recoverWith
      case ex =>
        Future.failed(ex)
    }

  private[this] def getOrCreateRelationship(
    client: ManagementClient,
    operatorSeed: OperatorSeed
  ): Future[Relationship] = {
    // TODO These could be replaced by a PUT
    def createPersonIfMissing: Future[Person] =
      recoverIfMissing(
        partyManagementService.getPersonByTaxCode(operatorSeed.taxCode),
        partyManagementService.createPerson(PersonSeed(operatorSeed.taxCode, operatorSeed.surname, operatorSeed.name))
      )

    def createRelationship: Future[Relationship] = for {
      _        <- createPersonIfMissing
      consumer <- partyManagementService.getOrganization(client.consumerId)
      relationship <- partyManagementService.createRelationship(
        RelationshipSeed(
          operatorSeed.taxCode,
          consumer.institutionId,
          RelationshipSeedEnums.Role.Operator,
          PartyManagementService.ROLE_SECURITY_OPERATOR
        )
      )
    } yield relationship

    for {
      maybeRelationship <- securityOperatorRelationship(client.consumerId, operatorSeed.taxCode)
      relationship <- maybeRelationship match {
        case Some(relationship) => Future.successful(relationship)
        case None               => createRelationship
      }
    } yield relationship
  }

  /** Code: 204, Message: Operator removed
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client or operator not found, DataType: Problem
    * Code: 500, Message: Internal server error, DataType: Problem
    */
  override def removeClientOperator(clientId: String, operatorTaxCode: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _                 <- extractBearer(contexts)
      clientUuid        <- toUuid(clientId).toFuture
      client            <- authorizationManagementService.getClient(clientId)
      maybeRelationship <- securityOperatorRelationship(client.consumerId, operatorTaxCode)
      relationship <- maybeRelationship.toFuture(
        SecurityOperatorRelationshipNotFound(client.consumerId.toString, operatorTaxCode)
      )
      _ <- authorizationManagementService.removeClientRelationship(clientUuid, relationship.id)
    } yield ()

    onComplete(result) {
      case Success(_) => removeClientOperator204
      case Failure(ex @ UnauthenticatedError) =>
        removeClientOperator401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: UuidConversionError) =>
        removeClientOperator400(Problem(Option(ex.getMessage), 400, "Bad request"))
      case Failure(ex: SecurityOperatorRelationshipNotFound) =>
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
    toEntityMarshallerClientKey: ToEntityMarshaller[ClientKey],
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
    toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _          <- extractBearer(contexts)
      clientUuid <- toUuid(clientId).toFuture
      client     <- authorizationManagementService.getClient(clientId)
      relationshipsIds <- keysSeeds.traverse(seed =>
        securityOperatorRelationship(client.consumerId, seed.operatorTaxCode)
      )
      seeds <- keysSeeds
        .zip(relationshipsIds)
        .traverse {
          case (seed, Some(relationship)) =>
            AuthorizationManagementService.toClientKeySeed(seed, relationship.id)
          case (seed, None) =>
            Left(SecurityOperatorRelationshipNotFound(client.consumerId.toString, seed.operatorTaxCode))
        }
        .toFuture
      keysResponse <- authorizationManagementService.createKeys(clientUuid, seeds)
    } yield ClientKeys(keysResponse.keys.map(AuthorizationManagementService.keyToApi))

    onComplete(result) {
      case Success(keys)                      => createKeys201(keys)
      case Failure(ex @ UnauthenticatedError) => createKeys401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: EnumParameterError)    => createKeys400(Problem(Option(ex.getMessage), 400, "Bad Request"))
      case Failure(ex: SecurityOperatorRelationshipNotFound) =>
        createKeys403(Problem(Option(ex.getMessage), 403, "Forbidden"))
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
    toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _            <- extractBearer(contexts)
      clientUuid   <- toUuid(clientId).toFuture
      keysResponse <- authorizationManagementService.getClientKeys(clientUuid)
    } yield ClientKeys(keysResponse.keys.map(AuthorizationManagementService.keyToApi))

    onComplete(result) {
      case Success(keys)                      => getClientKeys200(keys)
      case Failure(ex @ UnauthenticatedError) => getClientKeys401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        getClientKeys404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(ex) => getClientKeys500(Problem(Option(ex.getMessage), 500, "Error on client keys retrieve"))
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
    val result = for {
      _         <- extractBearer(contexts)
      client    <- authorizationManagementService.getClient(clientId)
      operators <- operatorsFromClient(client)
    } yield operators

    onComplete(result) {
      case Success(keys) => getClientOperators200(keys)
      case Failure(ex @ UnauthenticatedError) =>
        getClientOperators401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        getClientOperators404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(ex) => complete((500, Problem(Option(ex.getMessage), 500, "Error on client keys retrieve")))
    }
  }

  /** Code: 200, Message: Client Operator retrieved, DataType: Operator
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client or Operator not found, DataType: Problem
    */
  override def getClientOperatorByExternalId(clientId: String, operatorTaxCode: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerOperator: ToEntityMarshaller[Operator],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _         <- extractBearer(contexts)
      client    <- authorizationManagementService.getClient(clientId)
      operators <- operatorsFromClient(client)
      operator <- operators
        .find(_.taxCode == operatorTaxCode)
        .toFuture(SecurityOperatorRelationshipNotFound(client.consumerId.toString, operatorTaxCode))
    } yield operator

    onComplete(result) {
      case Success(operator) => getClientOperatorByExternalId200(operator)
      case Failure(ex @ UnauthenticatedError) =>
        getClientOperatorByExternalId401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        getClientOperatorByExternalId404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(ex: SecurityOperatorRelationshipNotFound) =>
        getClientOperatorByExternalId404(Problem(Option(ex.getMessage), 404, "Not found"))
      case Failure(ex) => complete((500, Problem(Option(ex.getMessage), 500, "Error on client keys retrieve")))
    }
  }

  private[this] def getClient(bearerToken: String, client: keymanagement.client.model.Client): Future[Client] = {
    for {
      eService   <- catalogManagementService.getEService(bearerToken, client.eServiceId.toString)
      producer   <- partyManagementService.getOrganization(eService.producerId)
      consumer   <- partyManagementService.getOrganization(client.consumerId)
      operators  <- operatorsFromClient(client)
      agreements <- agreementManagementService.getAgreements(bearerToken, consumer.partyId, eService.id.toString, None)
      agreement  <- getLatestAgreement(agreements, eService).toFuture(ClientAgreementNotFoundError(client.id.toString))
      client     <- clientToApi(client, eService, producer, consumer, agreement, operators)
    } yield client
  }

  private[this] def securityOperatorRelationship(
    consumerId: UUID,
    taxCode: String
  ): Future[Option[partymanagement.client.model.Relationship]] =
    for {
      consumer <- partyManagementService.getOrganization(consumerId)
      relationships <-
        partyManagementService
          .getRelationships(consumer.institutionId, taxCode, PartyManagementService.ROLE_SECURITY_OPERATOR)
          .map(Some(_))
          // TODO This is dangerous because every error is treated as "missing party with given tax code"
          //  but currently there is no precise way to identify the error
          .recoverWith(_ => Future.successful(None))
      activeRelationships = relationships.toSeq.flatMap(_.items.filter(_.status == RelationshipEnums.Status.Active))
      securityOperatorRel = activeRelationships.headOption // Only one expected
    } yield securityOperatorRel

  private[this] def operatorsFromClient(client: keymanagement.client.model.Client): Future[Seq[Operator]] =
    client.relationships.toSeq.traverse(operatorFromRelationship)

  private[this] def operatorFromRelationship(relationshipId: UUID): Future[Operator] =
    for {
      relationship <- partyManagementService.getRelationshipById(relationshipId)
      person       <- partyManagementService.getPersonByTaxCode(relationship.from)
    } yield Operator(
      taxCode = person.taxCode,
      name = person.name,
      surname = person.surname,
      role = relationship.role.toString,
      platformRole = relationship.platformRole
    )

  private[this] def compareDescriptorsVersion(
    descriptor1: EServiceDescriptor,
    descriptor2: EServiceDescriptor
  ): Boolean =
    // TODO Use createdAt timestamp once available
    descriptor1.version.toInt > descriptor2.version.toInt

  private[this] def getLatestAgreement(
    agreements: Seq[agreementmanagement.client.model.Agreement],
    eService: catalogmanagement.client.model.EService
  ): Option[agreementmanagement.client.model.Agreement] = {
    val activeAgreement = agreements.find(_.status == AgreementEnums.Status.Active)
    lazy val latestAgreementByDescriptor =
      agreements
        .map(agreement => (agreement, eService.descriptors.find(_.id == agreement.descriptorId)))
        .collect { case (agreement, Some(descriptor)) =>
          (agreement, descriptor)
        }
        .sortWith((elem1, elem2) => compareDescriptorsVersion(elem1._2, elem2._2))
        .map(_._1)
        .headOption

    activeAgreement.orElse(latestAgreementByDescriptor)
  }

  private[this] def clientToApi(
    client: keymanagement.client.model.Client,
    eService: catalogmanagement.client.model.EService,
    provider: partymanagement.client.model.Organization,
    consumer: partymanagement.client.model.Organization,
    agreement: agreementmanagement.client.model.Agreement,
    operator: Seq[Operator]
  ): Future[Client] = {
    for {
      agreementDescriptor <- eService.descriptors
        .find(_.id == agreement.descriptorId)
        .toRight(
          UnknownAgreementDescriptor(
            agreement.id.toString,
            agreement.eserviceId.toString,
            agreement.descriptorId.toString
          )
        )
        .toFuture
      apiAgreementDescriptor = CatalogManagementService.descriptorToApi(agreementDescriptor)
      apiProvider            = PartyManagementService.organizationToApi(provider)
      activeDescriptor       = CatalogManagementService.getActiveDescriptor(eService)
    } yield Client(
      id = client.id,
      eservice = CatalogManagementService.eServiceToApi(eService, apiProvider, activeDescriptor),
      consumer = PartyManagementService.organizationToApi(consumer),
      agreement = AgreementManagementService.agreementToApi(agreement, apiAgreementDescriptor),
      name = client.name,
      purposes = client.purposes,
      description = client.description,
      operators = Some(operator)
    )
  }

}
