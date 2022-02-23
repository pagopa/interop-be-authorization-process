package it.pagopa.pdnd.interop.uservice.authorizationprocess.util

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import com.nimbusds.jwt.JWTClaimsSet
import it.pagopa.interop.authorizationmanagement
import it.pagopa.interop.authorizationmanagement.client.{model => AuthorizationManagementDependency}
import it.pagopa.pdnd.interop.commons.jwt.service.JWTReader
import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.model.{Agreement => AgreementManagerAgreement}
import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.{model => AgreementManagementDependency}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.client.{model => CatalogManagementDependency}
import it.pagopa.pdnd.interop.uservice.partymanagement.client.{model => PartyManagementDependency}
import it.pagopa.pdnd.interop.uservice.purposemanagement.client.{model => PurposeManagementDependency}
import it.pagopa.pdnd.interop.uservice.userregistrymanagement.client.model.Certification.NONE
import it.pagopa.pdnd.interop.uservice.userregistrymanagement.client.model.{User, UserExtras}
import org.scalamock.scalatest.MockFactory

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.Future
import scala.util.Success

trait SpecUtils extends SprayJsonSupport { self: MockFactory =>

  val mockAgreementManagementService: AgreementManagementService         = mock[AgreementManagementService]
  val mockCatalogManagementService: CatalogManagementService             = mock[CatalogManagementService]
  val mockAuthorizationManagementService: AuthorizationManagementService = mock[AuthorizationManagementService]
  val mockPartyManagementService: PartyManagementService                 = mock[PartyManagementService]
  val mockPurposeManagementService: PurposeManagementService             = mock[PurposeManagementService]
  val mockUserRegistryManagementService: UserRegistryManagementService   = mock[UserRegistryManagementService]
  val mockJwtReader: JWTReader                                           = mock[JWTReader]

  val timestamp: OffsetDateTime = OffsetDateTime.now()

  def mockSubject(uuid: String): Success[JWTClaimsSet] = Success(new JWTClaimsSet.Builder().subject(uuid).build())

  val bearerToken: String    = "token"
  val eServiceId: UUID       = UUID.randomUUID()
  val consumerId: UUID       = UUID.randomUUID()
  val agreementId: UUID      = UUID.randomUUID()
  val organizationId: UUID   = UUID.randomUUID()
  val personId: UUID         = UUID.randomUUID()
  val taxCode: String        = "taxCode"
  val institutionId: String  = "some-external-id1"
  val clientSeed: ClientSeed = ClientSeed(organizationId, "client name", Some("client description"))
  val user: User = User(
    id = personId,
    externalId = taxCode,
    surname = "Surname",
    name = "Name",
    certification = NONE,
    extras = UserExtras(email = None, birthDate = None)
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
    dailyCallsMaxNumber = 1000
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
    verifiedAttributes = Seq.empty,
    createdAt = timestamp
  )

  val organization: PartyManagementDependency.Organization = PartyManagementDependency.Organization(
    institutionId = institutionId,
    description = "Organization description",
    digitalAddress = "or2@test.pec.pagopa.it",
    id = organizationId,
    attributes = Seq.empty,
    taxCode = "123",
    address = "address",
    zipCode = "00000"
  )

  val consumer: PartyManagementDependency.Organization = PartyManagementDependency.Organization(
    institutionId = "some-external-id2",
    description = "Organization description",
    digitalAddress = "org2@test.pec.pagopa.it",
    id = consumerId,
    attributes = Seq.empty,
    taxCode = "123",
    address = "address",
    zipCode = "00000"
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

  val clientPurpose: AuthorizationManagementDependency.Purpose =
    AuthorizationManagementDependency.Purpose(
      purposeId = UUID.randomUUID(),
      states = AuthorizationManagementDependency.ClientStatesChain(
        id = UUID.randomUUID(),
        eservice = AuthorizationManagementDependency.ClientEServiceDetails(
          eserviceId = UUID.randomUUID(),
          state = AuthorizationManagementDependency.ClientComponentState.ACTIVE,
          audience = Seq("audience"),
          voucherLifespan = 10
        ),
        agreement = AuthorizationManagementDependency.ClientAgreementDetails(
          agreementId = UUID.randomUUID(),
          state = AuthorizationManagementDependency.ClientComponentState.ACTIVE
        ),
        purpose = AuthorizationManagementDependency.ClientPurposeDetails(
          purposeId = UUID.randomUUID(),
          state = AuthorizationManagementDependency.ClientComponentState.ACTIVE
        )
      )
    )

  val client: AuthorizationManagementDependency.Client =
    AuthorizationManagementDependency.Client(
      id = UUID.randomUUID(),
      consumerId = consumerId,
      name = clientSeed.name,
      purposes = Seq(clientPurpose),
      description = clientSeed.description,
      relationships = Set.empty,
      kind = AuthorizationManagementDependency.ClientKind.CONSUMER
    )

  val operator: Operator =
    Operator(
      relationshipId = UUID.fromString(relationshipId),
      taxCode = user.externalId,
      name = user.name,
      surname = user.surname,
      role = OperatorRole.OPERATOR,
      product = RelationshipProduct("PDND", "aPlatformRole", timestamp),
      state = OperatorState.ACTIVE
    )

  val relationship: PartyManagementDependency.Relationship = PartyManagementDependency.Relationship(
    id = UUID.fromString(relationshipId),
    from = user.id,
    to = organization.id,
    role = PartyManagementDependency.PartyRole.OPERATOR,
    product = PartyManagementDependency.RelationshipProduct("PDND", "aPlatformRole", timestamp),
    state = PartyManagementDependency.RelationshipState.ACTIVE,
    createdAt = timestamp
  )

  val relationships: PartyManagementDependency.Relationships =
    PartyManagementDependency.Relationships(Seq(relationship))

  val createdKey: AuthorizationManagementDependency.ClientKey = AuthorizationManagementDependency.ClientKey(
    relationshipId = UUID.randomUUID(),
    name = "test",
    createdAt = OffsetDateTime.now(),
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

  def mockClientComposition(
    withOperators: Boolean,
    client: authorizationmanagement.client.model.Client = client,
    relationship: PartyManagementDependency.Relationship = relationship
  ): Unit = {

    (mockPartyManagementService
      .getOrganization(_: UUID)(_: String))
      .expects(client.consumerId, bearerToken)
      .once()
      .returns(Future.successful(consumer))

    if (withOperators) {
      (mockPartyManagementService
        .getRelationshipById(_: UUID)(_: String))
        .expects(relationship.id, bearerToken)
        .once()
        .returns(Future.successful(relationship))

      (mockUserRegistryManagementService.getUserById _)
        .expects(relationship.from)
        .once()
        .returns(Future.successful(user))
    }

    ()
  }

  val clientApiMarshaller: ClientApiMarshallerImpl.type = ClientApiMarshallerImpl

  implicit val contexts: Seq[(String, String)] = Seq("bearer" -> bearerToken, "uid" -> personId.toString)

  implicit def fromResponseUnmarshallerClientRequest: FromEntityUnmarshaller[Client] =
    sprayJsonUnmarshaller[Client]

  implicit def fromResponseUnmarshallerClientSeqRequest: FromEntityUnmarshaller[Seq[Client]] =
    sprayJsonUnmarshaller[Seq[Client]]

  implicit def fromResponseUnmarshallerClientsRequest: FromEntityUnmarshaller[Clients] =
    sprayJsonUnmarshaller[Clients]

  implicit def fromResponseUnmarshallerClientKeyRequest: FromEntityUnmarshaller[ClientKey] =
    sprayJsonUnmarshaller[ClientKey]

  implicit def fromResponseUnmarshallerClientKeysRequest: FromEntityUnmarshaller[ClientKeys] =
    sprayJsonUnmarshaller[ClientKeys]

  implicit def fromResponseUnmarshallerOperatorsRequest: FromEntityUnmarshaller[Seq[Operator]] =
    sprayJsonUnmarshaller[Seq[Operator]]

  implicit def fromResponseUnmarshallerOperatorRequest: FromEntityUnmarshaller[Operator] =
    sprayJsonUnmarshaller[Operator]

}
