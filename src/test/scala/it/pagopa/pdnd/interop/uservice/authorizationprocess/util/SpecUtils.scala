package it.pagopa.pdnd.interop.uservice.authorizationprocess.util

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.model.{Agreement, AgreementEnums}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.{AuthApiMarshallerImpl, _}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.{Client, ClientSeed, Key, Keys}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service._
import it.pagopa.pdnd.interop.uservice.keymanagement.client.model.{
  OtherPrimeInfo,
  Client => AuthManagementClient,
  Key => AuthManagementKey
}
import org.scalamock.scalatest.MockFactory

import java.util.UUID

trait SpecUtils extends SprayJsonSupport { self: MockFactory =>

  val mockJwtValidator: JWTValidator                                     = mock[JWTValidator]
  val mockJwtGenerator: JWTGenerator                                     = mock[JWTGenerator]
  val mockAgreementProcessService: AgreementProcessService               = mock[AgreementProcessService]
  val mockAgreementManagementService: AgreementManagementService         = mock[AgreementManagementService]
  val mockAuthorizationManagementService: AuthorizationManagementService = mock[AuthorizationManagementService]

  val bearerToken: String    = "token"
  val agreementId: UUID      = UUID.randomUUID()
  val clientSeed: ClientSeed = ClientSeed(agreementId, "client description")

  val validAgreement: Agreement = Agreement(
    id = agreementId,
    eserviceId = UUID.randomUUID(),
    producerId = UUID.randomUUID(),
    consumerId = UUID.randomUUID(),
    status = AgreementEnums.Status.Active,
    verifiedAttributes = Seq.empty
  )

  val suspendedAgreement: Agreement = Agreement(
    id = agreementId,
    eserviceId = UUID.randomUUID(),
    producerId = UUID.randomUUID(),
    consumerId = UUID.randomUUID(),
    status = AgreementEnums.Status.Suspended,
    verifiedAttributes = Seq.empty
  )

  val createdClient: AuthManagementClient =
    AuthManagementClient(
      id = UUID.randomUUID(),
      agreementId = agreementId,
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

  implicit def fromResponseUnmarshallerClientSeqRequest: FromEntityUnmarshaller[Seq[Client]] =
    sprayJsonUnmarshaller[Seq[Client]]

  implicit def fromResponseUnmarshallerKeyRequest: FromEntityUnmarshaller[Key] =
    sprayJsonUnmarshaller[Key]

  implicit def fromResponseUnmarshallerKeysRequest: FromEntityUnmarshaller[Keys] =
    sprayJsonUnmarshaller[Keys]

}
