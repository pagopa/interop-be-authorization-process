package it.pagopa.interop.authorizationprocess.util

import cats.syntax.all._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import com.nimbusds.jwt.JWTClaimsSet
import it.pagopa.interop.agreementmanagement.client.model.{Stamps, Agreement => AgreementManagerAgreement}
import it.pagopa.interop.agreementmanagement.client.{model => AgreementManagementDependency}
import it.pagopa.interop.authorizationprocess.{model => AuthorizationProcessModel}
import it.pagopa.interop.authorizationmanagement
import it.pagopa.interop.authorizationmanagement.client.{model => AuthorizationManagementDependency}
import it.pagopa.interop.authorizationmanagement.model.{client => AuthorizationPersistentModel}
import it.pagopa.interop.authorizationprocess.api.impl.{ClientApiMarshallerImpl, _}
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationprocess.service._
import it.pagopa.interop.catalogmanagement.client.{model => CatalogManagementDependency}
import it.pagopa.interop.commons.utils.USER_ROLES
import it.pagopa.interop.purposemanagement.client.{model => PurposeManagementDependency}
import it.pagopa.interop.selfcare.partymanagement.client.{model => PartyManagementDependency}
import it.pagopa.interop.selfcare.userregistry.client.model.{
  CertifiableFieldResourceOfstring,
  CertifiableFieldResourceOfstringEnums,
  UserResource
}
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import org.scalamock.scalatest.MockFactory

import java.time.{OffsetDateTime, Duration}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success
import it.pagopa.interop.tenantmanagement.client.model.Tenant
import it.pagopa.interop.tenantmanagement.client.model.ExternalId
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier

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
  val clientSeed: ClientSeed = ClientSeed("client name", Some("client description"))
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

  val activeDescriptor: CatalogManagementDependency.EServiceDescriptor = CatalogManagementDependency.EServiceDescriptor(
    id = UUID.randomUUID(),
    version = "1",
    description = None,
    interface = None,
    docs = Seq.empty,
    state = CatalogManagementDependency.EServiceDescriptorState.PUBLISHED,
    audience = Seq.empty,
    voucherLifespan = 10,
    dailyCallsPerConsumer = 1000,
    dailyCallsTotal = 20,
    agreementApprovalPolicy = CatalogManagementDependency.AgreementApprovalPolicy.AUTOMATIC,
    serverUrls = Nil
  )

  val eService: CatalogManagementDependency.EService = CatalogManagementDependency.EService(
    id = eServiceId,
    producerId = organizationId,
    name = "Service name",
    description = "Service description",
    technology = CatalogManagementDependency.EServiceTechnology.REST,
    attributes = CatalogManagementDependency.Attributes(Seq.empty, Seq.empty, Seq.empty),
    descriptors = Seq(activeDescriptor)
  )

  val agreement: AgreementManagerAgreement = AgreementManagerAgreement(
    id = agreementId,
    eserviceId = eServiceId,
    descriptorId = activeDescriptor.id,
    producerId = organizationId,
    consumerId = consumerId,
    state = AgreementManagementDependency.AgreementState.ACTIVE,
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
    stamps = Stamps()
  )

  val purposeVersion: PurposeManagementDependency.PurposeVersion = PurposeManagementDependency.PurposeVersion(
    id = UUID.randomUUID(),
    state = PurposeManagementDependency.PurposeVersionState.ACTIVE,
    createdAt = timestamp,
    updatedAt = None,
    firstActivationAt = None,
    expectedApprovalDate = None,
    dailyCalls = 10,
    riskAnalysis = None
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
      id = UUID.randomUUID(),
      eService = AuthorizationPersistentModel.PersistentClientEServiceDetails(
        eServiceId = eServiceId,
        descriptorId = descriptorId,
        state = AuthorizationPersistentModel.PersistentClientComponentState.Active,
        audience = Seq("audience"),
        voucherLifespan = 0
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

  val consumer: Tenant = Tenant(
    id = client.consumerId,
    selfcareId = UUID.randomUUID.toString.some,
    externalId = ExternalId("IPA", "value"),
    features = Nil,
    attributes = Nil,
    createdAt = OffsetDateTimeSupplier.get(),
    updatedAt = None,
    mails = Nil,
    name = "test_name"
  )

  val purpose: PurposeManagementDependency.Purpose = PurposeManagementDependency.Purpose(
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
    updatedAt = None
  )

  val archivedPurpose: PurposeManagementDependency.Purpose =
    purpose.copy(
      id = UUID.randomUUID(),
      versions = Seq(
        purposeVersion.copy(
          id = UUID.randomUUID,
          state = PurposeManagementDependency.PurposeVersionState.ACTIVE,
          createdAt = OffsetDateTime.now().minus(Duration.ofDays(10))
        ),
        purposeVersion.copy(
          id = UUID.randomUUID,
          state = PurposeManagementDependency.PurposeVersionState.ARCHIVED,
          createdAt = OffsetDateTime.now()
        )
      )
    )

  val notArchivedPurpose: PurposeManagementDependency.Purpose =
    purpose.copy(
      id = UUID.randomUUID(),
      versions = Seq(
        purposeVersion.copy(
          id = UUID.randomUUID,
          state = PurposeManagementDependency.PurposeVersionState.ACTIVE,
          createdAt = OffsetDateTime.now()
        ),
        purposeVersion.copy(
          id = UUID.randomUUID,
          state = PurposeManagementDependency.PurposeVersionState.ARCHIVED,
          createdAt = OffsetDateTime.now().minus(Duration.ofDays(10))
        )
      )
    )

  val operator: Operator = Operator(
    relationshipId = UUID.fromString(relationshipId),
    taxCode = user.fiscalCode.get,
    name = user.name.get.value,
    familyName = user.familyName.get.value,
    role = OperatorRole.OPERATOR,
    product = RelationshipProduct("Interop", "aPlatformRole", timestamp),
    state = OperatorState.ACTIVE
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

  val createdKey: AuthorizationManagementDependency.ClientKey = AuthorizationManagementDependency.ClientKey(
    relationshipId = UUID.randomUUID(),
    name = "test",
    createdAt = timestamp,
    key = AuthorizationManagementDependency.Key(
      kty = "1",
      keyOps = Some(Seq("2")),
      use = Some("3"),
      alg = Some("4"),
      kid = "5",
      x5u = Some("6"),
      x5t = Some("7"),
      x5tS256 = Some("8"),
      x5c = Some(Seq("9")),
      crv = Some("10"),
      x = Some("11"),
      y = Some("12"),
      d = Some("13"),
      k = Some("14"),
      n = Some("15"),
      e = Some("16"),
      p = Some("17"),
      q = Some("18"),
      dp = Some("19"),
      dq = Some("20"),
      qi = Some("21"),
      oth = Some(Seq(AuthorizationManagementDependency.OtherPrimeInfo("22", "23", "24")))
    )
  )

  def mockGetTenant(): Unit = (mockTenantManagementService
    .getTenant(_: UUID)(_: Seq[(String, String)]))
    .expects(client.consumerId, *)
    .once()
    .returns(Future.successful(consumer)): Unit

  def mockClientComposition(
    withOperators: Boolean,
    client: authorizationmanagement.client.model.Client = client,
    relationship: PartyManagementDependency.Relationship = relationship
  )(implicit contexts: Seq[(String, String)]): Unit = {

    (mockTenantManagementService
      .getTenant(_: UUID)(_: Seq[(String, String)]))
      .expects(client.consumerId, *)
      .once()
      .returns(Future.successful(consumer))

    (mockAgreementManagementService
      .getAgreements(_: UUID, _: UUID)(_: Seq[(String, String)]))
      .expects(client.purposes.head.states.eservice.eserviceId, client.consumerId, contexts)
      .once()
      .returns(Future.successful(Seq(agreement)))

    client.purposes.foreach { clientPurpose =>
      (mockPurposeManagementService
        .getPurpose(_: UUID)(_: Seq[(String, String)]))
        .expects(clientPurpose.states.purpose.purposeId, contexts)
        .once()
        .returns(Future.successful(purpose.copy(eserviceId = eService.id, consumerId = consumer.id)))

      (mockCatalogManagementService
        .getEService(_: UUID)(_: Seq[(String, String)]))
        .expects(agreement.eserviceId, contexts)
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

  implicit def fromResponseUnmarshallerClientKeyRequest: FromEntityUnmarshaller[ClientKey] =
    sprayJsonUnmarshaller[ClientKey]

  implicit def fromResponseUnmarshallerReadClientKeyRequest: FromEntityUnmarshaller[ReadClientKey] =
    sprayJsonUnmarshaller[ReadClientKey]

  implicit def fromResponseUnmarshallerClientKeysRequest: FromEntityUnmarshaller[ClientKeys] =
    sprayJsonUnmarshaller[ClientKeys]

  implicit def fromResponseUnmarshallerReadClientKeysRequest: FromEntityUnmarshaller[ReadClientKeys] =
    sprayJsonUnmarshaller[ReadClientKeys]

  implicit def fromResponseUnmarshallerOperatorsRequest: FromEntityUnmarshaller[Seq[Operator]] =
    sprayJsonUnmarshaller[Seq[Operator]]

  implicit def fromResponseUnmarshallerOperatorRequest: FromEntityUnmarshaller[Operator] =
    sprayJsonUnmarshaller[Operator]

  implicit def fromResponseUnmarshallerProblem: FromEntityUnmarshaller[Problem] =
    sprayJsonUnmarshaller[Problem]

}
