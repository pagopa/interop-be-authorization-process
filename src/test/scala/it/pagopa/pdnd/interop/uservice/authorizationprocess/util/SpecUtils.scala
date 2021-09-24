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
  ClientKey => AuthManagementClientKey,
  Key => AuthManagementKey
}
import it.pagopa.pdnd.interop.uservice.partymanagement.client.model.{
  Person,
  Relationship,
  RelationshipEnums,
  Relationships,
  Organization => PartyManagementOrganization
}
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

  val bearerToken: String        = "token"
  val eServiceId: UUID           = UUID.randomUUID()
  val consumerId: UUID           = UUID.randomUUID()
  val agreementId: UUID          = UUID.randomUUID()
  val organizationId: UUID       = UUID.randomUUID()
  val personId: UUID             = UUID.randomUUID()
  val clientSeed: ClientSeed     = ClientSeed(eServiceId, consumerId, "client name", Some("client description"))
  val taxCode: String            = "taxCode"
  val person: Person             = Person(taxCode = taxCode, surname = "Surname", name = "Name", partyId = personId.toString)
  val operatorSeed: OperatorSeed = OperatorSeed(person.taxCode, person.name, person.surname)

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
    institutionId = "some-external-id1",
    description = "Organization description",
    managerName = "ManagerName",
    managerSurname = "ManagerSurname",
    digitalAddress = "or2@test.pec.pagopa.it",
    partyId = organizationId.toString,
    attributes = Seq.empty
  )

  val consumer: PartyManagementOrganization = PartyManagementOrganization(
    institutionId = "some-external-id2",
    description = "Organization description",
    managerName = "ManagerName",
    managerSurname = "ManagerSurname",
    digitalAddress = "org2@test.pec.pagopa.it",
    partyId = consumerId.toString,
    attributes = Seq.empty
  )

  val client: AuthManagementClient =
    AuthManagementClient(
      id = UUID.randomUUID(),
      eServiceId = eServiceId,
      consumerId = consumerId,
      clientSeed.name,
      clientSeed.description,
      relationships = Set.empty
    )

  val operator: Operator =
    Operator(
      taxCode = person.taxCode,
      name = person.name,
      surname = person.surname,
      role = "Operator",
      platformRole = "aPlatformRole"
    )

  val relationship: Relationship = Relationship(
    id = UUID.randomUUID(),
    from = person.taxCode,
    to = organization.institutionId,
    role = RelationshipEnums.Role.Operator,
    platformRole = "aPlatformRole",
    status = RelationshipEnums.Status.Active
  )

  val relationships: Relationships = Relationships(Seq(relationship))

  val createdKey: AuthManagementClientKey = AuthManagementClientKey(
    status = ClientKeyEnums.Status.Active,
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
    relationship: Relationship = relationship,
    eService: CatalogManagementEService = eService,
    agreements: Seq[AgreementManagerAgreement] = Seq(agreement)
  ): Unit = {

    (mockCatalogManagementService.getEService _)
      .expects(*, client.eServiceId.toString)
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

      (mockPartyManagementService.getPersonByTaxCode _)
        .expects(relationship.from)
        .once()
        .returns(Future.successful(person))
    }

    (mockAgreementManagementService.getAgreements _)
      .expects(*, client.consumerId.toString, client.eServiceId.toString, None)
      .once()
      .returns(Future.successful(agreements))

    ()
  }

  val clientApiMarshaller: ClientApiMarshallerImpl = new ClientApiMarshallerImpl()

  implicit val contexts: Seq[(String, String)] = Seq("bearer" -> bearerToken)

  implicit def fromResponseUnmarshallerClientRequest: FromEntityUnmarshaller[Client] =
    sprayJsonUnmarshaller[Client]

  implicit def fromResponseUnmarshallerClientSeqRequest: FromEntityUnmarshaller[Seq[Client]] =
    sprayJsonUnmarshaller[Seq[Client]]

  implicit def fromResponseUnmarshallerClientKeyRequest: FromEntityUnmarshaller[ClientKey] =
    sprayJsonUnmarshaller[ClientKey]

  implicit def fromResponseUnmarshallerClientKeysRequest: FromEntityUnmarshaller[ClientKeys] =
    sprayJsonUnmarshaller[ClientKeys]

  implicit def fromResponseUnmarshallerOperatorRequest: FromEntityUnmarshaller[Seq[Operator]] =
    sprayJsonUnmarshaller[Seq[Operator]]

}
