package it.pagopa.pdnd.interop.uservice.authorizationprocess.util

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.model.{
  AgreementEnums,
  Agreement => AgreementManagerAgreement
}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service._
import it.pagopa.pdnd.interop.uservice.catalogmanagement.client.model.{
  Attributes,
  EServiceDescriptorEnums,
  EService => CatalogManagementEService,
  EServiceDescriptor => CatalogManagementDescriptor
}
import it.pagopa.pdnd.interop.uservice.keymanagement
import it.pagopa.pdnd.interop.uservice.keymanagement.client.model.{
  ClientKeyEnums,
  OtherPrimeInfo,
  Client => AuthManagementClient,
  ClientEnums => AuthManagementClientEnums,
  ClientKey => AuthManagementClientKey,
  Key => AuthManagementKey
}
import it.pagopa.pdnd.interop.uservice.partymanagement.client.model.{
  Organization => PartyManagementOrganization,
  Relationship => PartyRelationship,
  RelationshipEnums => PartyRelationshipEnums,
  Relationships => PartyRelationships
}
import it.pagopa.pdnd.interop.uservice.userregistrymanagement.client.model.{NONE, User, UserExtras}
import org.scalamock.scalatest.MockFactory

import java.util.UUID
import scala.concurrent.Future

trait SpecUtils extends SprayJsonSupport { self: MockFactory =>

  val mockJwtValidator: JWTValidator                                     = mock[JWTValidator]
  val mockJwtGenerator: JWTGenerator                                     = mock[JWTGenerator]
  val mockAgreementManagementService: AgreementManagementService         = mock[AgreementManagementService]
  val mockCatalogManagementService: CatalogManagementService             = mock[CatalogManagementService]
  val mockAuthorizationManagementService: AuthorizationManagementService = mock[AuthorizationManagementService]
  val mockPartyManagementService: PartyManagementService                 = mock[PartyManagementService]
  val mockUserRegistryManagementService: UserRegistryManagementService   = mock[UserRegistryManagementService]
  val mockVaultService: VaultService                                     = mock[VaultService]

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
  val operatorSeed: OperatorSeed = OperatorSeed(user.externalId, user.name, user.surname)

  val activeDescriptor: CatalogManagementDescriptor = CatalogManagementDescriptor(
    id = UUID.randomUUID(),
    version = "1",
    description = None,
    interface = None,
    docs = Seq.empty,
    status = EServiceDescriptorEnums.Status.Published,
    audience = Seq.empty,
    voucherLifespan = 10
  )

  val eService: CatalogManagementEService = CatalogManagementEService(
    id = eServiceId,
    producerId = organizationId,
    name = "Service name",
    description = "Service description",
    technology = "REST",
    attributes = Attributes(Seq.empty, Seq.empty, Seq.empty),
    descriptors = Seq(activeDescriptor)
  )

  val agreement: AgreementManagerAgreement = AgreementManagerAgreement(
    id = agreementId,
    eserviceId = eServiceId,
    descriptorId = activeDescriptor.id,
    producerId = organizationId,
    consumerId = consumerId,
    status = AgreementEnums.Status.Active,
    verifiedAttributes = Seq.empty
  )

  val organization: PartyManagementOrganization = PartyManagementOrganization(
    institutionId = institutionId,
    description = "Organization description",
    digitalAddress = "or2@test.pec.pagopa.it",
    id = organizationId,
    attributes = Seq.empty
  )

  val consumer: PartyManagementOrganization = PartyManagementOrganization(
    institutionId = "some-external-id2",
    description = "Organization description",
    digitalAddress = "org2@test.pec.pagopa.it",
    id = consumerId,
    attributes = Seq.empty
  )

  val client: AuthManagementClient =
    AuthManagementClient(
      id = UUID.randomUUID(),
      eServiceId = eServiceId,
      consumerId = consumerId,
      name = clientSeed.name,
      purposes = clientSeed.purposes,
      description = clientSeed.description,
      status = AuthManagementClientEnums.Status.Active,
      relationships = Set.empty
    )

  val operator: Operator =
    Operator(
      id = user.id,
      taxCode = user.externalId,
      name = user.name,
      surname = user.surname,
      role = "Operator",
      platformRole = "aPlatformRole",
      status = "active"
    )

  val relationship: PartyRelationship = PartyRelationship(
    id = UUID.randomUUID(),
    from = user.id,
    to = organization.id,
    role = PartyRelationshipEnums.Role.Operator,
    platformRole = "aPlatformRole",
    status = PartyRelationshipEnums.Status.Active
  )

  val relationships: PartyRelationships = PartyRelationships(Seq(relationship))

  val createdKey: AuthManagementClientKey = AuthManagementClientKey(
    status = ClientKeyEnums.Status.Active,
    relationshipId = UUID.randomUUID(),
    key = AuthManagementKey(
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
      oth = Some(Seq(OtherPrimeInfo("22", "23", "24")))
    )
  )

  def mockClientComposition(
    withOperators: Boolean,
    client: keymanagement.client.model.Client = client,
    relationship: PartyRelationship = relationship,
    eService: CatalogManagementEService = eService,
    agreements: Seq[AgreementManagerAgreement] = Seq(agreement)
  ): Unit = {

    (mockCatalogManagementService.getEService _)
      .expects(*, client.eServiceId)
      .once()
      .returns(Future.successful(eService))

    (mockPartyManagementService.getOrganization _)
      .expects(eService.producerId)
      .once()
      .returns(Future.successful(organization))

    (mockPartyManagementService.getOrganization _)
      .expects(client.consumerId)
      .once()
      .returns(Future.successful(consumer))

    if (withOperators) {
      (mockPartyManagementService.getRelationshipById _)
        .expects(relationship.id)
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
