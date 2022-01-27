package it.pagopa.pdnd.interop.uservice.authorizationprocess.util

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import com.nimbusds.jwt.JWTClaimsSet
import it.pagopa.pdnd.interop.commons.jwt.service.JWTReader
import it.pagopa.pdnd.interop.commons.vault.service.VaultService
import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.model.{Agreement => AgreementManagerAgreement}
import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.{model => AgreementManagementDependency}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.client.{model => CatalogManagementDependency}
import it.pagopa.pdnd.interop.uservice.keymanagement
import it.pagopa.pdnd.interop.uservice.keymanagement.client.{model => AuthorizationManagementDependency}
import it.pagopa.pdnd.interop.uservice.partymanagement.client.{model => PartyManagementDependency}
import it.pagopa.pdnd.interop.uservice.userregistrymanagement.client.model.{User, UserExtras}
import it.pagopa.pdnd.interop.uservice.userregistrymanagement.client.model.Certification.NONE
import org.scalamock.scalatest.MockFactory

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.Future
import scala.util.Success

trait SpecUtils extends SprayJsonSupport { self: MockFactory =>

  System.setProperty("CATALOG_MANAGEMENT_URL", "localhost")
  System.setProperty("KEY_MANAGEMENT_URL", "localhost")
  System.setProperty("PARTY_MANAGEMENT_URL", "localhost")
  System.setProperty("AGREEMENT_MANAGEMENT_URL", "localhost")
  System.setProperty("USER_REGISTRY_MANAGEMENT_URL", "localhost")
  System.setProperty("VAULT_ADDR", "localhost")
  System.setProperty("VAULT_TOKEN", "TokenIlGurriero")
  System.setProperty("PDND_INTEROP_KEYS", "test/keys")
  System.setProperty("USER_REGISTRY_API_KEY", "meaow")
  System.setProperty("WELL_KNOWN_URL", "localhost/.well-known")

  val mockJwtValidator: JWTValidator                                     = mock[JWTValidator]
  val mockJwtGenerator: JWTGenerator                                     = mock[JWTGenerator]
  val mockAgreementManagementService: AgreementManagementService         = mock[AgreementManagementService]
  val mockCatalogManagementService: CatalogManagementService             = mock[CatalogManagementService]
  val mockAuthorizationManagementService: AuthorizationManagementService = mock[AuthorizationManagementService]
  val mockPartyManagementService: PartyManagementService                 = mock[PartyManagementService]
  val mockUserRegistryManagementService: UserRegistryManagementService   = mock[UserRegistryManagementService]
  val mockJwtReader: JWTReader                                           = mock[JWTReader]
  val mockVaultService: VaultService                                     = mock[VaultService]

  val timestamp: OffsetDateTime = OffsetDateTime.now()

  def mockSubject(uuid: String) = Success(new JWTClaimsSet.Builder().subject(uuid).build())

  val bearerToken: String   = "token"
  val eServiceId: UUID      = UUID.randomUUID()
  val consumerId: UUID      = UUID.randomUUID()
  val agreementId: UUID     = UUID.randomUUID()
  val organizationId: UUID  = UUID.randomUUID()
  val personId: UUID        = UUID.randomUUID()
  val taxCode: String       = "taxCode"
  val institutionId: String = "some-external-id1"
  val clientSeed: ClientSeed =
    ClientSeed(eServiceId, organizationId, "client name", "purposes", Some("client description"))
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
    voucherLifespan = 10
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
    verifiedAttributes = Seq.empty
  )

  val organization: PartyManagementDependency.Organization = PartyManagementDependency.Organization(
    institutionId = institutionId,
    description = "Organization description",
    digitalAddress = "or2@test.pec.pagopa.it",
    id = organizationId,
    attributes = Seq.empty,
    taxCode = "123"
  )

  val consumer: PartyManagementDependency.Organization = PartyManagementDependency.Organization(
    institutionId = "some-external-id2",
    description = "Organization description",
    digitalAddress = "org2@test.pec.pagopa.it",
    id = consumerId,
    attributes = Seq.empty,
    taxCode = "123"
  )

  val client: AuthorizationManagementDependency.Client =
    AuthorizationManagementDependency.Client(
      id = UUID.randomUUID(),
      eServiceId = eServiceId,
      consumerId = consumerId,
      name = clientSeed.name,
      purposes = clientSeed.purposes,
      description = clientSeed.description,
      state = AuthorizationManagementDependency.ClientState.ACTIVE,
      relationships = Set.empty
    )

  val operator: Operator =
    Operator(
      id = UUID.fromString(relationshipId),
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
    client: keymanagement.client.model.Client = client,
    relationship: PartyManagementDependency.Relationship = relationship,
    eService: CatalogManagementDependency.EService = eService,
    agreements: Seq[AgreementManagerAgreement] = Seq(agreement)
  ): Unit = {

    (mockCatalogManagementService.getEService _)
      .expects(*, client.eServiceId)
      .once()
      .returns(Future.successful(eService))

    (mockPartyManagementService
      .getOrganization(_: UUID)(_: String))
      .expects(eService.producerId, bearerToken)
      .once()
      .returns(Future.successful(organization))

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

    (mockAgreementManagementService.getAgreements _)
      .expects(*, client.consumerId, client.eServiceId, None)
      .once()
      .returns(Future.successful(agreements))

    ()
  }

  val clientApiMarshaller: ClientApiMarshallerImpl.type = ClientApiMarshallerImpl

  implicit val contexts: Seq[(String, String)] = Seq("bearer" -> bearerToken)

  implicit def fromResponseUnmarshallerClientRequest: FromEntityUnmarshaller[Client] =
    sprayJsonUnmarshaller[Client]

  implicit def fromResponseUnmarshallerClientSeqRequest: FromEntityUnmarshaller[Seq[Client]] =
    sprayJsonUnmarshaller[Seq[Client]]

  implicit def fromResponseUnmarshallerClientKeyRequest: FromEntityUnmarshaller[ClientKey] =
    sprayJsonUnmarshaller[ClientKey]

  implicit def fromResponseUnmarshallerClientKeysRequest: FromEntityUnmarshaller[ClientKeys] =
    sprayJsonUnmarshaller[ClientKeys]

  implicit def fromResponseUnmarshallerOperatorsRequest: FromEntityUnmarshaller[Seq[Operator]] =
    sprayJsonUnmarshaller[Seq[Operator]]

  implicit def fromResponseUnmarshallerOperatorRequest: FromEntityUnmarshaller[Operator] =
    sprayJsonUnmarshaller[Operator]

}
