package it.pagopa.pdnd.interop.uservice.authorizationprocess.util

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.{AuthApiMarshallerImpl, _}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service._
import it.pagopa.pdnd.interop.uservice.keymanagement.client.model.{
  OtherPrimeInfo,
  Client => AuthManagementClient,
  Key => AuthManagementKey
}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.client.model.{
  Attributes,
  EService => CatalogManagementEService
}
import it.pagopa.pdnd.interop.uservice.partymanagement.client.model.{Organization => PartyManagementOrganization}
import org.scalamock.scalatest.MockFactory

import java.util.UUID

trait SpecUtils extends SprayJsonSupport { self: MockFactory =>

  val mockJwtValidator: JWTValidator                                     = mock[JWTValidator]
  val mockJwtGenerator: JWTGenerator                                     = mock[JWTGenerator]
  val mockAgreementManagementService: AgreementManagementService         = mock[AgreementManagementService]
  val mockCatalogManagementService: CatalogManagementService             = mock[CatalogManagementService]
  val mockAuthorizationManagementService: AuthorizationManagementService = mock[AuthorizationManagementService]
  val mockPartyManagementService: PartyManagementService                 = mock[PartyManagementService]

  val bearerToken: String    = "token"
  val eServiceId: UUID       = UUID.randomUUID()
  val consumerId: UUID       = UUID.randomUUID()
  val organizationId: UUID   = UUID.randomUUID()
  val clientSeed: ClientSeed = ClientSeed(eServiceId, consumerId, "client name", Some("client description"))

  val eService: CatalogManagementEService = CatalogManagementEService(
    id = eServiceId,
    producerId = organizationId,
    name = "Service name",
    description = "Service description",
    audience = Seq.empty,
    technology = "REST",
    voucherLifespan = 10,
    attributes = Attributes(Seq.empty, Seq.empty, Seq.empty),
    descriptors = Seq.empty
  )

  val organization: PartyManagementOrganization = PartyManagementOrganization(
    institutionId = "some-external-id",
    description = "Organization description",
    managerName = "ManagerName",
    managerSurname = "ManagerSurname",
    digitalAddress = "org@test.pec.pagopa.it",
    partyId = organizationId.toString,
    attributes = Seq.empty
  )

  val client: AuthManagementClient =
    AuthManagementClient(
      id = UUID.randomUUID(),
      eServiceId = eServiceId,
      consumerId = consumerId,
      clientSeed.name,
      clientSeed.description,
      operators = Set.empty
    )

  val createdKey: AuthManagementKey = AuthManagementKey(
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

  val authApiMarshaller: AuthApiMarshallerImpl = new AuthApiMarshallerImpl()

  implicit val contexts: Seq[(String, String)] = Seq("bearer" -> bearerToken)

  implicit def fromResponseUnmarshallerClientRequest: FromEntityUnmarshaller[Client] =
    sprayJsonUnmarshaller[Client]

  implicit def fromResponseUnmarshallerClientDetailRequest: FromEntityUnmarshaller[ClientDetail] =
    sprayJsonUnmarshaller[ClientDetail]

  implicit def fromResponseUnmarshallerClientDetailSeqRequest: FromEntityUnmarshaller[Seq[ClientDetail]] =
    sprayJsonUnmarshaller[Seq[ClientDetail]]

  implicit def fromResponseUnmarshallerKeyRequest: FromEntityUnmarshaller[Key] =
    sprayJsonUnmarshaller[Key]

  implicit def fromResponseUnmarshallerKeysRequest: FromEntityUnmarshaller[Keys] =
    sprayJsonUnmarshaller[Keys]

}
