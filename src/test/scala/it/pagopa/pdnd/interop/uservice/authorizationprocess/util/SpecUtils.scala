package it.pagopa.pdnd.interop.uservice.authorizationprocess.util

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.model.{Agreement, AgreementEnums}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.{AuthApiMarshallerImpl, _}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.{Client, ClientSeed}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service._
import it.pagopa.pdnd.interop.uservice.keymanagement.client.model.{Client => AuthManagementClient}
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

  val authApiMarshaller: AuthApiMarshallerImpl = new AuthApiMarshallerImpl()

  implicit val contexts: Seq[(String, String)] = Seq("bearer" -> bearerToken)

  implicit def fromResponseUnmarshallerClientRequest: FromEntityUnmarshaller[Client] =
    sprayJsonUnmarshaller[Client]

}
