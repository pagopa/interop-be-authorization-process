package it.pagopa.interop.authorizationprocess.util

import it.pagopa.interop.agreementmanagement.client.model.Agreement
import it.pagopa.interop.authorizationmanagement.client.model._
import it.pagopa.interop.authorizationprocess.service._
import it.pagopa.interop.catalogmanagement.client.model.{Attributes, EService, EServiceTechnology}
import it.pagopa.interop.purposemanagement.client.model
import it.pagopa.interop.purposemanagement.client.model.PurposeVersion
import it.pagopa.interop.selfcare.partymanagement.client.model._
import it.pagopa.interop.selfcare.userregistry.client.model.UserResource

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

/**
 * Holds fake implementation of dependencies for tests not requiring neither mocks or stubs
 */
object FakeDependencies {
  class FakeAgreementManagementService     extends AgreementManagementService     {
    override def getAgreements(eServiceId: UUID, consumerId: UUID)(implicit
      contexts: Seq[(String, String)]
    ): Future[Seq[Agreement]] = Future.successful(Seq.empty)
  }
  class FakeCatalogManagementService       extends CatalogManagementService       {
    override def getEService(eServiceId: UUID)(implicit contexts: Seq[(String, String)]): Future[EService] =
      Future.successful(
        EService(
          id = UUID.randomUUID(),
          producerId = UUID.randomUUID(),
          name = "fake",
          description = "fake",
          technology = EServiceTechnology.REST,
          attributes = Attributes(Seq.empty, Seq.empty, Seq.empty),
          descriptors = Seq.empty
        )
      )
  }
  class FakeAuthorizationManagementService extends AuthorizationManagementService {
    override def createClient(consumerId: UUID, name: String, description: Option[String], kind: ClientKind)(implicit
      contexts: Seq[(String, String)]
    ): Future[ManagementClient] = Future.successful(
      Client(
        id = UUID.randomUUID(),
        consumerId = UUID.randomUUID(),
        name = "fake",
        purposes = Seq.empty,
        relationships = Set.empty,
        kind = ClientKind.API
      )
    )

    override def getClient(clientId: UUID)(implicit contexts: Seq[(String, String)]): Future[ManagementClient] =
      Future.successful(
        Client(
          id = UUID.randomUUID(),
          consumerId = UUID.randomUUID(),
          name = "fake",
          purposes = Seq.empty,
          relationships = Set.empty,
          kind = ClientKind.API
        )
      )

    override def listClients(
      offset: Option[Int],
      limit: Option[Int],
      relationshipId: Option[UUID],
      consumerId: Option[UUID],
      purposeId: Option[UUID],
      kind: Option[ClientKind]
    )(implicit contexts: Seq[(String, String)]): Future[Seq[ManagementClient]] = Future.successful(Seq.empty)

    override def deleteClient(clientId: UUID)(implicit contexts: Seq[(String, String)]): Future[Unit] =
      Future.successful(())

    override def addRelationship(clientId: UUID, relationshipId: UUID)(implicit
      contexts: Seq[(String, String)]
    ): Future[ManagementClient] = Future.successful(
      Client(
        id = UUID.randomUUID(),
        consumerId = UUID.randomUUID(),
        name = "fake",
        purposes = Seq.empty,
        relationships = Set.empty,
        kind = ClientKind.API
      )
    )

    override def removeClientRelationship(clientId: UUID, relationshipId: UUID)(implicit
      contexts: Seq[(String, String)]
    ): Future[Unit] = Future.successful(())

    override def getKey(clientId: UUID, kid: String)(implicit contexts: Seq[(String, String)]): Future[ClientKey] =
      Future.successful(
        ClientKey(
          key = Key(kty = "fake", kid = "fake"),
          relationshipId = UUID.randomUUID(),
          name = "fake",
          createdAt = OffsetDateTime.now()
        )
      )

    override def getClientKeys(clientId: UUID)(implicit contexts: Seq[(String, String)]): Future[KeysResponse] =
      Future.successful(KeysResponse(Seq.empty))

    override def createKeys(clientId: UUID, keysSeeds: Seq[KeySeed])(implicit
      contexts: Seq[(String, String)]
    ): Future[KeysResponse] = Future.successful(KeysResponse(Seq.empty))

    override def deleteKey(clientId: UUID, kid: String)(implicit contexts: Seq[(String, String)]): Future[Unit] =
      Future.successful(())

    override def getEncodedClientKey(clientId: UUID, kid: String)(implicit
      contexts: Seq[(String, String)]
    ): Future[EncodedClientKey] = Future.successful(EncodedClientKey("fake"))

    override def addClientPurpose(clientId: UUID, purposeSeed: PurposeSeed)(implicit
      contexts: Seq[(String, String)]
    ): Future[Purpose] = Future.successful(
      Purpose(
        purposeId = UUID.randomUUID(),
        states = ClientStatesChain(
          id = UUID.randomUUID(),
          eservice = ClientEServiceDetails(
            eserviceId = UUID.randomUUID(),
            state = ClientComponentState.ACTIVE,
            audience = Seq.empty,
            voucherLifespan = 1000
          ),
          agreement = ClientAgreementDetails(
            eserviceId = UUID.randomUUID(),
            consumerId = UUID.randomUUID(),
            state = ClientComponentState.ACTIVE
          ),
          purpose = ClientPurposeDetails(purposeId = UUID.randomUUID(), state = ClientComponentState.ACTIVE)
        )
      )
    )

    override def removeClientPurpose(clientId: UUID, purposeId: UUID)(implicit
      contexts: Seq[(String, String)]
    ): Future[Unit] = Future.successful(())
  }
  class FakePartyManagementService         extends PartyManagementService         {
    override def getInstitution(
      institutionId: UUID
    )(implicit contexts: Seq[(String, String)], ec: ExecutionContext): Future[Institution] = Future.successful(
      Institution(
        id = UUID.randomUUID(),
        /* external institution id */
        externalId = "fake",
        /* origin institution id (e.g iPA code) */
        originId = "fake",
        description = "fake",
        digitalAddress = "fake",
        address = "fake",
        zipCode = "fake",
        /* institution tax code */
        taxCode = "fake",
        /* The origin form which the institution has been retrieved */
        origin = "fake",
        /* institution type */
        institutionType = "fake",
        attributes = Seq.empty
      )
    )

    override def getRelationships(organizationId: UUID, personId: UUID, productRoles: Seq[String])(implicit
      contexts: Seq[(String, String)],
      ec: ExecutionContext
    ): Future[Relationships] = Future.successful(Relationships(Seq.empty))

    override def getRelationshipsByPersonId(personId: UUID, productRole: Seq[String])(implicit
      contexts: Seq[(String, String)],
      ec: ExecutionContext
    ): Future[Relationships] = Future.successful(Relationships(Seq.empty))

    override def getRelationshipById(
      relationshipId: UUID
    )(implicit contexts: Seq[(String, String)], ec: ExecutionContext): Future[Relationship] = Future.successful(
      Relationship(
        id = UUID.randomUUID(),
        from = UUID.randomUUID(),
        to = UUID.randomUUID(),
        role = PartyRole.MANAGER,
        product = RelationshipProduct(id = "fake", role = "fake", createdAt = OffsetDateTime.now()),
        state = RelationshipState.ACTIVE,
        createdAt = OffsetDateTime.now()
      )
    )
  }
  class FakePurposeManagementService       extends PurposeManagementService       {
    override def getPurpose(purposeId: UUID)(implicit contexts: Seq[(String, String)]): Future[model.Purpose] =
      Future.successful(
        model.Purpose(
          id = UUID.randomUUID(),
          eserviceId = UUID.randomUUID(),
          consumerId = UUID.randomUUID(),
          versions = Seq.empty[PurposeVersion],
          title = "fake",
          description = "fake",
          createdAt = OffsetDateTime.now()
        )
      )
  }
  class FakeUserRegistryManagementService  extends UserRegistryManagementService  {
    override def getUserById(id: UUID)(implicit contexts: Seq[(String, String)]): Future[UserResource] =
      Future.successful(UserResource(id = UUID.randomUUID()))
  }
}
