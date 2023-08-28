package it.pagopa.interop.authorizationprocess.util

import cats.syntax.all._
import it.pagopa.interop.authorizationmanagement.client.model._
import it.pagopa.interop.authorizationprocess.service._
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.selfcare.partymanagement.client.model._
import it.pagopa.interop.selfcare.userregistry.client.model.UserResource
import it.pagopa.interop.catalogmanagement.model.{CatalogItem, CatalogAttributes, Rest}
import it.pagopa.interop.authorizationmanagement.model.client.{PersistentClientKind, PersistentClient, Api}
import it.pagopa.interop.authorizationmanagement.model.key.{PersistentKey, Sig}
import it.pagopa.interop.agreementmanagement.model.agreement.PersistentAgreement
import it.pagopa.interop.purposemanagement.model.purpose.PersistentPurpose
import it.pagopa.interop.tenantmanagement.model.tenant.{PersistentTenant, PersistentTenantKind, PersistentExternalId}
import it.pagopa.interop.authorizationprocess.common.readmodel.PaginatedResult
import it.pagopa.interop.authorizationprocess.common.readmodel.model.ReadModelClientWithKeys

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

/**
 * Holds fake implementation of dependencies for tests not requiring neither mocks or stubs
 */
object FakeDependencies {
  class FakeAgreementManagementService     extends AgreementManagementService     {
    override def getAgreements(eServiceId: UUID, consumerId: UUID)(implicit
      ec: ExecutionContext,
      readModel: ReadModelService
    ): Future[Seq[PersistentAgreement]] = Future.successful(Seq.empty)
  }
  class FakeCatalogManagementService       extends CatalogManagementService       {
    override def getEServiceById(
      eServiceId: UUID
    )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[CatalogItem] =
      Future.successful(
        CatalogItem(
          id = UUID.randomUUID(),
          producerId = UUID.randomUUID(),
          name = "fake",
          description = "fake",
          technology = Rest,
          attributes = CatalogAttributes(Seq.empty, Seq.empty, Seq.empty).some,
          descriptors = Seq.empty,
          createdAt = OffsetDateTimeSupplier.get()
        )
      )
  }
  class FakeAuthorizationManagementService extends AuthorizationManagementService {
    override def createClient(
      consumerId: UUID,
      name: String,
      description: Option[String],
      kind: ClientKind,
      createdAt: OffsetDateTime,
      members: Seq[UUID]
    )(implicit contexts: Seq[(String, String)]): Future[ManagementClient] = Future.successful(
      Client(
        id = UUID.randomUUID(),
        consumerId = UUID.randomUUID(),
        name = "fake",
        purposes = Seq.empty,
        relationships = Set.empty,
        kind = ClientKind.API,
        createdAt = createdAt
      )
    )

    override def getClient(
      clientId: UUID
    )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PersistentClient] =
      Future.successful(
        PersistentClient(
          id = UUID.randomUUID(),
          consumerId = UUID.randomUUID(),
          name = "fake",
          description = Some("description"),
          purposes = Seq.empty,
          relationships = Set.empty,
          kind = Api,
          createdAt = OffsetDateTimeSupplier.get()
        )
      )

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
        kind = ClientKind.API,
        createdAt = OffsetDateTimeSupplier.get()
      )
    )

    override def removeClientRelationship(clientId: UUID, relationshipId: UUID)(implicit
      contexts: Seq[(String, String)]
    ): Future[Unit] = Future.successful(())

    override def getClientKey(clientId: UUID, kid: String)(implicit
      ec: ExecutionContext,
      readModel: ReadModelService
    ): Future[PersistentKey] =
      Future.successful(
        PersistentKey(
          relationshipId = UUID.randomUUID(),
          kid = "fake",
          name = "fake",
          encodedPem = "pem",
          algorithm = "algorithm",
          use = Sig,
          createdAt = OffsetDateTimeSupplier.get()
        )
      )

    override def getClientKeys(
      clientId: UUID
    )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[Seq[PersistentKey]] =
      Future.successful(Seq.empty)

    override def createKeys(clientId: UUID, keysSeeds: Seq[KeySeed])(implicit
      contexts: Seq[(String, String)]
    ): Future[Keys] = Future.successful(Keys(Seq.empty))

    override def deleteKey(clientId: UUID, kid: String)(implicit contexts: Seq[(String, String)]): Future[Unit] =
      Future.successful(())

    override def addClientPurpose(clientId: UUID, purposeSeed: PurposeSeed)(implicit
      contexts: Seq[(String, String)]
    ): Future[Purpose] = Future.successful(
      Purpose(states =
        ClientStatesChain(
          id = UUID.randomUUID(),
          eservice = ClientEServiceDetails(
            eserviceId = UUID.randomUUID(),
            descriptorId = UUID.randomUUID(),
            state = ClientComponentState.ACTIVE,
            audience = Seq.empty,
            voucherLifespan = 1000
          ),
          agreement = ClientAgreementDetails(
            eserviceId = UUID.randomUUID(),
            consumerId = UUID.randomUUID(),
            agreementId = UUID.randomUUID(),
            state = ClientComponentState.ACTIVE
          ),
          purpose = ClientPurposeDetails(
            purposeId = UUID.randomUUID(),
            versionId = UUID.randomUUID(),
            state = ClientComponentState.ACTIVE
          )
        )
      )
    )

    override def removeClientPurpose(clientId: UUID, purposeId: UUID)(implicit
      contexts: Seq[(String, String)]
    ): Future[Unit] = Future.successful(())

    override def getClients(
      name: Option[String],
      relationshipIds: List[UUID],
      consumerId: UUID,
      purposeId: Option[UUID],
      kind: Option[PersistentClientKind],
      offset: Int,
      limit: Int
    )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PaginatedResult[PersistentClient]] =
      Future.successful(PaginatedResult(results = Seq.empty, totalCount = 0))

    override def getClientsByPurpose(
      purposeId: UUID
    )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[Seq[PersistentClient]] =
      Future.successful(Seq.empty)

    override def getClientsWithKeys(
      name: Option[String],
      relationshipIds: List[UUID],
      consumerId: UUID,
      purposeId: Option[UUID],
      kind: Option[PersistentClientKind],
      offset: Int,
      limit: Int
    )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PaginatedResult[ReadModelClientWithKeys]] =
      Future.successful(PaginatedResult(results = Seq.empty, totalCount = 0))
  }
  class FakePartyManagementService         extends PartyManagementService         {

    override def getRelationships(organizationId: String, personId: UUID, productRoles: Seq[String])(implicit
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
        product = RelationshipProduct(id = "fake", role = "fake", createdAt = OffsetDateTimeSupplier.get()),
        state = RelationshipState.ACTIVE,
        createdAt = OffsetDateTimeSupplier.get().some
      )
    )
  }
  class FakePurposeManagementService      extends PurposeManagementService      {
    override def getPurposeById(
      purposeId: UUID
    )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PersistentPurpose] =
      Future.successful(
        PersistentPurpose(
          id = UUID.randomUUID(),
          eserviceId = UUID.randomUUID(),
          consumerId = UUID.randomUUID(),
          versions = Seq.empty,
          title = "fake",
          description = "fake",
          createdAt = OffsetDateTimeSupplier.get(),
          isFreeOfCharge = true,
          suspendedByConsumer = None,
          suspendedByProducer = None,
          riskAnalysisForm = None,
          updatedAt = Some(OffsetDateTimeSupplier.get()),
          freeOfChargeReason = None
        )
      )
  }
  class FakeUserRegistryManagementService extends UserRegistryManagementService {
    override def getUserById(id: UUID)(implicit contexts: Seq[(String, String)]): Future[UserResource] =
      Future.successful(UserResource(id = UUID.randomUUID()))
  }
  class FakeTenantManagementService       extends TenantManagementService       {
    override def getTenantById(
      tenantId: UUID
    )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PersistentTenant] =
      Future.successful(
        PersistentTenant(
          id = tenantId,
          selfcareId = UUID.randomUUID().toString.some,
          externalId = PersistentExternalId("IPA", "foo"),
          features = Nil,
          attributes = Nil,
          createdAt = OffsetDateTimeSupplier.get(),
          updatedAt = None,
          mails = Nil,
          name = "test_name",
          kind = Some(PersistentTenantKind.PA)
        )
      )
  }
}
