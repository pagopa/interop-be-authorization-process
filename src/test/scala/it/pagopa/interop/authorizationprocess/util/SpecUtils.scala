package it.pagopa.interop.authorizationprocess.util

import cats.syntax.all._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import com.nimbusds.jwt.JWTClaimsSet
import it.pagopa.interop.authorizationprocess.{model => AuthorizationProcessModel}
import it.pagopa.interop.authorizationmanagement
import it.pagopa.interop.authorizationmanagement.client.{model => AuthorizationManagementDependency}
import it.pagopa.interop.authorizationmanagement.model.{client => AuthorizationPersistentModel}
import it.pagopa.interop.authorizationmanagement.model.{key => AuthorizationPersistentKeyModel}
import it.pagopa.interop.authorizationprocess.api.impl.{ClientApiMarshallerImpl, _}
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationprocess.service._
import it.pagopa.interop.commons.utils.USER_ROLES
import it.pagopa.interop.selfcare.partymanagement.client.{model => PartyManagementDependency}
import it.pagopa.interop.selfcare.userregistry.client.model.{
  CertifiableFieldResourceOfstring,
  CertifiableFieldResourceOfstringEnums,
  UserResource
}
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import it.pagopa.interop.catalogmanagement.model.{
  Automatic,
  CatalogAttributes,
  CatalogDescriptor,
  CatalogItem,
  Published,
  Rest,
  Deliver
}
import it.pagopa.interop.agreementmanagement.model.agreement.{Active, PersistentAgreement, PersistentStamps}
import it.pagopa.interop.authorizationmanagement.client.model.KeyUse.SIG
import it.pagopa.interop.authorizationprocess.common.Adapters.PersistentKeyUseWrapper
import it.pagopa.interop.purposemanagement.model.purpose.{
  Archived,
  PersistentPurpose,
  PersistentPurposeVersion,
  Active => PurposeActive
}
import it.pagopa.interop.tenantmanagement.model.tenant.{PersistentExternalId, PersistentTenant, PersistentTenantKind}
import org.scalamock.scalatest.MockFactory

import java.time.{Duration, OffsetDateTime}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

trait SpecUtilsWithImplicit extends SpecUtils {
  self: MockFactory =>

  implicit val contexts: Seq[(String, String)] =
    Seq(
      "bearer"         -> bearerToken,
      "uid"            -> personId.toString,
      USER_ROLES       -> "admin",
      "organizationId" -> consumerId.toString
    )
}

trait SpecUtils extends SprayJsonSupport { self: MockFactory =>

  val mockAgreementManagementService: AgreementManagementService         = mock[AgreementManagementService]
  val mockCatalogManagementService: CatalogManagementService             = mock[CatalogManagementService]
  val mockAuthorizationManagementService: AuthorizationManagementService = mock[AuthorizationManagementService]
  val mockPartyManagementService: PartyManagementService                 = mock[PartyManagementService]
  val mockPurposeManagementService: PurposeManagementService             = mock[PurposeManagementService]
  val mockUserRegistryManagementService: UserRegistryManagementService   = mock[UserRegistryManagementService]
  val mockTenantManagementService: TenantManagementService               = mock[TenantManagementService]
  val mockDateTimeSupplier: OffsetDateTimeSupplier                       = mock[OffsetDateTimeSupplier]
  val mockReadModel: ReadModelService                                    = mock[ReadModelService]

  val timestamp: OffsetDateTime = OffsetDateTime.now()

  def mockSubject(uuid: String): Success[JWTClaimsSet] = Success(new JWTClaimsSet.Builder().subject(uuid).build())

  val bearerToken: String    = "token"
  val eServiceId: UUID       = UUID.randomUUID()
  val consumerId: UUID       = UUID.randomUUID()
  val agreementId: UUID      = UUID.randomUUID()
  val organizationId: UUID   = UUID.randomUUID()
  val personId: UUID         = UUID.randomUUID()
  val descriptorId: UUID     = UUID.randomUUID()
  val versionId: UUID        = UUID.randomUUID()
  val purposeId: UUID        = UUID.randomUUID()
  val clientPurposeId: UUID  = UUID.randomUUID()
  val taxCode: String        = "taxCode"
  val institutionId: String  = "some-external-id1"
  val clientSeed: ClientSeed = ClientSeed("client name", Some("client description"), Seq.empty)
  val user: UserResource     = UserResource(
    id = personId,
    fiscalCode = Some(taxCode),
    familyName = Some(
      CertifiableFieldResourceOfstring(
        certification = CertifiableFieldResourceOfstringEnums.Certification.NONE,
        value = "Surname"
      )
    ),
    name = Some(
      CertifiableFieldResourceOfstring(
        certification = CertifiableFieldResourceOfstringEnums.Certification.NONE,
        value = "Name"
      )
    ),
    birthDate = None,
    email = None,
    workContacts = None
  )

  val relationshipId: String = UUID.randomUUID().toString

  val activeDescriptor: CatalogDescriptor = CatalogDescriptor(
    id = UUID.randomUUID(),
    version = "1",
    description = None,
    interface = None,
    docs = Seq.empty,
    state = Published,
    audience = Seq.empty,
    voucherLifespan = 10,
    dailyCallsPerConsumer = 1000,
    dailyCallsTotal = 20,
    agreementApprovalPolicy = Automatic.some,
    serverUrls = Nil,
    createdAt = timestamp,
    publishedAt = timestamp.some,
    suspendedAt = None,
    deprecatedAt = None,
    archivedAt = None,
    attributes = CatalogAttributes.empty
  )

  val eService: CatalogItem = CatalogItem(
    id = eServiceId,
    producerId = organizationId,
    name = "Service name",
    description = "Service description",
    technology = Rest,
    attributes = CatalogAttributes.empty.some,
    descriptors = Seq(activeDescriptor),
    createdAt = timestamp,
    riskAnalysis = Seq.empty,
    mode = Deliver
  )

  val agreement: PersistentAgreement = PersistentAgreement(
    id = agreementId,
    eserviceId = eServiceId,
    descriptorId = activeDescriptor.id,
    producerId = organizationId,
    consumerId = consumerId,
    state = Active,
    verifiedAttributes = List.empty,
    certifiedAttributes = List.empty,
    declaredAttributes = List.empty,
    suspendedByConsumer = None,
    suspendedByProducer = None,
    suspendedByPlatform = None,
    consumerDocuments = List.empty,
    createdAt = timestamp,
    updatedAt = None,
    consumerNotes = None,
    stamps = PersistentStamps(),
    contract = None,
    rejectionReason = None,
    suspendedAt = None
  )

  val purposeVersion: PersistentPurposeVersion = PersistentPurposeVersion(
    id = UUID.randomUUID(),
    state = PurposeActive,
    createdAt = timestamp,
    updatedAt = None,
    firstActivationAt = None,
    expectedApprovalDate = None,
    dailyCalls = 10,
    riskAnalysis = None,
    suspendedAt = None
  )

  val clientStateId = UUID.randomUUID()

  val clientPurposeProcess: AuthorizationProcessModel.ClientPurpose = AuthorizationProcessModel.ClientPurpose(states =
    AuthorizationProcessModel.ClientStatesChain(
      id = clientStateId,
      eservice = AuthorizationProcessModel.ClientEServiceDetails(
        eserviceId = eServiceId,
        descriptorId = descriptorId,
        state = AuthorizationProcessModel.ClientComponentState.ACTIVE,
        audience = Seq("audience"),
        voucherLifespan = 10
      ),
      agreement = AuthorizationProcessModel.ClientAgreementDetails(
        eserviceId = eServiceId,
        agreementId = agreementId,
        consumerId = consumerId,
        state = AuthorizationProcessModel.ClientComponentState.ACTIVE
      ),
      purpose = AuthorizationProcessModel.ClientPurposeDetails(
        purposeId = purposeId,
        versionId = versionId,
        state = AuthorizationProcessModel.ClientComponentState.ACTIVE
      )
    )
  )

  val clientPurpose: AuthorizationManagementDependency.Purpose = AuthorizationManagementDependency.Purpose(states =
    AuthorizationManagementDependency.ClientStatesChain(
      id = clientStateId,
      eservice = AuthorizationManagementDependency.ClientEServiceDetails(
        eserviceId = eServiceId,
        descriptorId = descriptorId,
        state = AuthorizationManagementDependency.ClientComponentState.ACTIVE,
        audience = Seq("audience"),
        voucherLifespan = 10
      ),
      agreement = AuthorizationManagementDependency.ClientAgreementDetails(
        eserviceId = eServiceId,
        consumerId = consumerId,
        agreementId = agreementId,
        state = AuthorizationManagementDependency.ClientComponentState.ACTIVE
      ),
      purpose = AuthorizationManagementDependency.ClientPurposeDetails(
        purposeId = purposeId,
        versionId = versionId,
        state = AuthorizationManagementDependency.ClientComponentState.ACTIVE
      )
    )
  )

  val persistentClientPurpose: AuthorizationPersistentModel.PersistentClientStatesChain =
    AuthorizationPersistentModel.PersistentClientStatesChain(
      id = clientStateId,
      eService = AuthorizationPersistentModel.PersistentClientEServiceDetails(
        eServiceId = eServiceId,
        descriptorId = descriptorId,
        state = AuthorizationPersistentModel.PersistentClientComponentState.Active,
        audience = Seq("audience"),
        voucherLifespan = 10
      ),
      agreement = AuthorizationPersistentModel.PersistentClientAgreementDetails(
        eServiceId = eServiceId,
        consumerId = consumerId,
        agreementId = agreementId,
        state = AuthorizationPersistentModel.PersistentClientComponentState.Active
      ),
      purpose = AuthorizationPersistentModel.PersistentClientPurposeDetails(
        purposeId = purposeId,
        versionId = versionId,
        state = AuthorizationPersistentModel.PersistentClientComponentState.Active
      )
    )

  val persistentClient: AuthorizationPersistentModel.PersistentClient = AuthorizationPersistentModel.PersistentClient(
    id = UUID.randomUUID(),
    consumerId = consumerId,
    name = clientSeed.name,
    purposes = Seq(persistentClientPurpose),
    description = clientSeed.description,
    relationships = Set.empty,
    kind = AuthorizationPersistentModel.Consumer,
    createdAt = timestamp
  )

  val persistentKey: AuthorizationPersistentKeyModel.PersistentKey = AuthorizationPersistentKeyModel.PersistentKey(
    kid = "QyiGZU3L-bbyWpJvp3UG5jSFXEuxoYlRdZeuf5o6ULI",
    encodedPem =
      "LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0KTUlJQklqQU5CZ2txaGtpRzl3MEJBUUVGQUFPQ0FROEFNSUlCQ2dLQ0FRRUF2RElqdVN4UkJtaEdudjE5MUlSTQpnbEVXblRFMXdmOXdMRzdwQStxQlBjckVyM3dQQWJoTzJab1ZpVFNLS1crWGlZbW15cS8zaVlkYlhXNVNLc1NqCnNEN1NWTEhzZ0YzWU85MjZpV0tLTGVWdThhOEdEcUx1K1ZrQjlDNGMxUWZLajJRRG1rNTN1OGlKOU12Mi84c28KVzY2VXM2NVM0TTlzc1Jka0ZzMUoxVWhQSVgxT1I3UjlBSnZKWFN2ZmtMekhvOHdveTVkM3JZdmJNMzErWk0wbwplL0tQdUdCVWRnRitreXNLZVE3eVgxM3NFK1NCaVZaRkJFYzdzd0xXRDIxeEZJSVlpWHdWTEFteC9lajBMMFNTCkVSUEsvSVpmRlN6UW92bE5vNVhsR3BGcStTWk5ZdlVyWTBRRndtK3M0UnN5R3lUOTJnWHBmaVpJeHZMMUI1TmgKZndJREFRQUIKLS0tLS1FTkQgUFVCTElDIEtFWS0tLS0t",
    algorithm = "RS256",
    use = AuthorizationPersistentKeyModel.Sig,
    relationshipId = UUID.randomUUID(),
    name = "test",
    createdAt = timestamp
  )

  val client: AuthorizationManagementDependency.Client = AuthorizationManagementDependency.Client(
    id = UUID.randomUUID(),
    consumerId = consumerId,
    name = clientSeed.name,
    purposes = Seq(clientPurpose),
    description = clientSeed.description,
    relationships = Set.empty,
    kind = AuthorizationManagementDependency.ClientKind.CONSUMER,
    createdAt = timestamp
  )

  val consumer: PersistentTenant = PersistentTenant(
    id = client.consumerId,
    selfcareId = UUID.randomUUID.toString.some,
    externalId = PersistentExternalId("IPA", "value"),
    features = Nil,
    attributes = Nil,
    createdAt = OffsetDateTimeSupplier.get(),
    updatedAt = None,
    mails = Nil,
    name = "test_name",
    kind = PersistentTenantKind.PA.some
  )

  val purpose: PersistentPurpose = PersistentPurpose(
    id = UUID.randomUUID(),
    eserviceId = eService.id,
    consumerId = consumer.id,
    versions = Seq.empty,
    suspendedByConsumer = None,
    suspendedByProducer = None,
    title = "Purpose!",
    description = "Purpose?",
    riskAnalysisForm = None,
    createdAt = timestamp,
    updatedAt = None,
    isFreeOfCharge = true,
    freeOfChargeReason = None
  )

  val archivedPurpose: PersistentPurpose =
    purpose.copy(
      id = UUID.randomUUID(),
      versions = Seq(
        purposeVersion.copy(
          id = UUID.randomUUID,
          state = PurposeActive,
          createdAt = OffsetDateTime.now().minus(Duration.ofDays(10))
        ),
        purposeVersion.copy(id = UUID.randomUUID, state = Archived, createdAt = OffsetDateTime.now())
      )
    )

  val notArchivedPurpose: PersistentPurpose =
    purpose.copy(
      id = UUID.randomUUID(),
      versions = Seq(
        purposeVersion.copy(id = UUID.randomUUID, state = PurposeActive, createdAt = OffsetDateTime.now()),
        purposeVersion.copy(
          id = UUID.randomUUID,
          state = Archived,
          createdAt = OffsetDateTime.now().minus(Duration.ofDays(10))
        )
      )
    )

  val relationship: PartyManagementDependency.Relationship = PartyManagementDependency.Relationship(
    id = UUID.fromString(relationshipId),
    from = user.id,
    to = organizationId,
    role = PartyManagementDependency.PartyRole.OPERATOR,
    product = PartyManagementDependency.RelationshipProduct("Interop", "aPlatformRole", timestamp),
    state = PartyManagementDependency.RelationshipState.ACTIVE,
    createdAt = timestamp.some
  )

  val relationships: PartyManagementDependency.Relationships =
    PartyManagementDependency.Relationships(Seq(relationship))

  val createdKey: AuthorizationManagementDependency.Key = AuthorizationManagementDependency.Key(
    relationshipId = persistentKey.relationshipId,
    kid = persistentKey.kid,
    name = persistentKey.name,
    encodedPem = persistentKey.encodedPem,
    algorithm = persistentKey.algorithm,
    use = SIG,
    createdAt = persistentKey.createdAt
  )

  val expectedKey: Key = Key(
    relationshipId = relationship.id,
    kid = persistentKey.kid,
    name = persistentKey.name,
    encodedPem = persistentKey.encodedPem,
    algorithm = persistentKey.algorithm,
    use = persistentKey.use.toApi,
    createdAt = persistentKey.createdAt
  )

  def mockGetTenant(): Unit = (mockTenantManagementService
    .getTenantById(_: UUID)(_: ExecutionContext, _: ReadModelService))
    .expects(client.consumerId, *, *)
    .once()
    .returns(Future.successful(consumer)): Unit

  def mockClientComposition(
    withOperators: Boolean,
    client: authorizationmanagement.client.model.Client = client,
    relationship: PartyManagementDependency.Relationship = relationship
  ): Unit = {

    (mockTenantManagementService
      .getTenantById(_: UUID)(_: ExecutionContext, _: ReadModelService))
      .expects(client.consumerId, *, *)
      .once()
      .returns(Future.successful(consumer))

    (mockAgreementManagementService
      .getAgreements(_: UUID, _: UUID)(_: ExecutionContext, _: ReadModelService))
      .expects(client.purposes.head.states.eservice.eserviceId, client.consumerId, *, *)
      .once()
      .returns(Future.successful(Seq(agreement)))

    client.purposes.foreach { clientPurpose =>
      (mockPurposeManagementService
        .getPurposeById(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(clientPurpose.states.purpose.purposeId, *, *)
        .once()
        .returns(Future.successful(purpose.copy(eserviceId = eService.id, consumerId = consumer.id)))

      (mockCatalogManagementService
        .getEServiceById(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(agreement.eserviceId, *, *)
        .once()
        .returns(
          Future.successful(eService.copy(descriptors = Seq(activeDescriptor.copy(id = agreement.descriptorId))))
        )

    }

    if (withOperators) {
      (mockPartyManagementService
        .getRelationshipById(_: UUID)(_: Seq[(String, String)], _: ExecutionContext))
        .expects(relationship.id, *, *)
        .once()
        .returns(Future.successful(relationship))

      (mockUserRegistryManagementService
        .getUserById(_: UUID)(_: Seq[(String, String)]))
        .expects(relationship.from, *)
        .once()
        .returns(Future.successful(user))
    }

    ()
  }

  val clientApiMarshaller: ClientApiMarshallerImpl.type = ClientApiMarshallerImpl

  val operatorApiMarshaller: OperatorApiMarshallerImpl.type = OperatorApiMarshallerImpl

  implicit def fromResponseUnmarshallerClientRequest: FromEntityUnmarshaller[Client] =
    sprayJsonUnmarshaller[Client]

  implicit def fromResponseUnmarshallerClientSeqRequest: FromEntityUnmarshaller[Seq[Client]] =
    sprayJsonUnmarshaller[Seq[Client]]

  implicit def fromResponseUnmarshallerClientsRequest: FromEntityUnmarshaller[Clients] =
    sprayJsonUnmarshaller[Clients]

  implicit def fromResponseUnmarshallerOperatorsRequest: FromEntityUnmarshaller[Seq[Operator]] =
    sprayJsonUnmarshaller[Seq[Operator]]

  implicit def fromResponseUnmarshallerOperatorRequest: FromEntityUnmarshaller[Operator] =
    sprayJsonUnmarshaller[Operator]

  implicit def fromResponseUnmarshallerProblem: FromEntityUnmarshaller[Problem] =
    sprayJsonUnmarshaller[Problem]

}
