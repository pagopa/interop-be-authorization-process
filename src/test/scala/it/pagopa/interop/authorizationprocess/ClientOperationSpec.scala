package it.pagopa.interop.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.authorizationmanagement
import it.pagopa.interop.authorizationmanagement.client.api.{ClientApi, KeyApi, PurposeApi}
import it.pagopa.interop.authorizationprocess.api.impl.ClientApiServiceImpl
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationprocess.service.impl.AuthorizationManagementServiceImpl
import it.pagopa.interop.authorizationprocess.service.{
  AuthorizationManagementInvoker,
  AuthorizationManagementService,
  CatalogManagementService,
  PartyManagementService
}
import it.pagopa.interop.authorizationprocess.util.SpecUtilsWithImplicit
import it.pagopa.interop.selfcare.partymanagement.client.model.Relationships
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class ClientOperationSpec extends AnyWordSpecLike with MockFactory with SpecUtilsWithImplicit with ScalatestRouteTest {

  import clientApiMarshaller._

  val service: ClientApiServiceImpl = ClientApiServiceImpl(
    mockAuthorizationManagementService,
    mockAgreementManagementService,
    mockCatalogManagementService,
    mockPartyManagementService,
    mockPurposeManagementService,
    mockUserRegistryManagementService
  )(ExecutionContext.global)

  "Client creation" should {
    "succeed" in {
      (mockAuthorizationManagementService
        .createClient(_: UUID, _: String, _: Option[String], _: authorizationmanagement.client.model.ClientKind)(
          _: Seq[(String, String)]
        ))
        .expects(
          consumerId,
          clientSeed.name,
          clientSeed.description,
          authorizationmanagement.client.model.ClientKind.CONSUMER,
          *
        )
        .once()
        .returns(Future.successful(client))

      mockClientComposition(withOperators = false)

      val expectedAgreement: Agreement = Agreement(
        id = agreement.id,
        eservice = CatalogManagementService.eServiceToApi(eService),
        descriptor = CatalogManagementService.descriptorToApi(activeDescriptor.copy(id = agreement.descriptorId))
      )

      val expected = Client(
        id = client.id,
        consumer = Organization(consumer.originId, consumer.description),
        name = client.name,
        purposes =
          client.purposes.map(AuthorizationManagementService.purposeToApi(_, purpose.title, expectedAgreement)),
        description = client.description,
        operators = Some(Seq.empty),
        kind = ClientKind.CONSUMER
      )

      Get() ~> service.createConsumerClient(clientSeed) ~> check {
        status shouldEqual StatusCodes.Created
        entityAs[Client] shouldEqual expected
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      val service: ClientApiServiceImpl            = ClientApiServiceImpl(
        AuthorizationManagementServiceImpl(
          AuthorizationManagementInvoker(ExecutionContext.global),
          ClientApi(),
          KeyApi(),
          PurposeApi()
        ),
        mockAgreementManagementService,
        mockCatalogManagementService,
        mockPartyManagementService,
        mockPurposeManagementService,
        mockUserRegistryManagementService
      )(ExecutionContext.global)
      Get() ~> service.createConsumerClient(clientSeed) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }
  }

  "Client retrieve" should {
    "succeed" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(*, *)
        .once()
        .returns(Future.successful(client))

      mockClientComposition(withOperators = false, client)

      val expectedAgreement: Agreement = Agreement(
        id = agreement.id,
        eservice = CatalogManagementService.eServiceToApi(eService),
        descriptor = CatalogManagementService.descriptorToApi(activeDescriptor.copy(id = agreement.descriptorId))
      )

      val expected =
        Client(
          id = client.id,
          consumer = Organization(consumer.originId, consumer.description),
          name = client.name,
          purposes =
            client.purposes.map(AuthorizationManagementService.purposeToApi(_, purpose.title, expectedAgreement)),
          description = client.description,
          operators = Some(Seq.empty),
          kind = ClientKind.CONSUMER
        )

      Get() ~> service.getClient(client.id.toString) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Client] shouldEqual expected
      }
    }

    "fail if the organization in the token is not the same in the client" in {
      val anotherConsumerId = UUID.randomUUID()

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(*, *)
        .once()
        .returns(Future.successful(client.copy(consumerId = anotherConsumerId)))

      Get() ~> service
        .getClient(client.id.toString)(contexts, toEntityMarshallerProblem, toEntityMarshallerClient) ~> check {
        status shouldEqual StatusCodes.Forbidden
        entityAs[
          Problem
        ].errors.head.detail shouldBe s"The resource client ${client.id} doesn't belong to the organization $anotherConsumerId"
      }
    }

    "fail if client does not exist" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(*, *)
        .once()
        .returns(Future.failed(authorizationmanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.getClient(client.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Client list" should {
    "succeed" in {
      val offset: Option[Int]            = Some(0)
      val limit: Option[Int]             = Some(10)
      val relationshipUuid: Option[UUID] = Some(relationship.id)
      val consumerUuid: Option[UUID]     = Some(client.consumerId)
      val purposeUuid: Option[UUID]      = Some(clientPurpose.states.purpose.purposeId)

      (mockPartyManagementService
        .getRelationships(_: UUID, _: UUID, _: Seq[String])(_: Seq[(String, String)], _: ExecutionContext))
        .expects(
          consumerId,
          personId,
          Seq(PartyManagementService.PRODUCT_ROLE_SECURITY_OPERATOR, PartyManagementService.PRODUCT_ROLE_ADMIN),
          *,
          *
        )
        .once()
        .returns(Future.successful(Relationships(Seq(relationship))))

      (mockAgreementManagementService
        .getAgreements(_: UUID, _: UUID)(_: Seq[(String, String)]))
        .expects(client.purposes.head.states.eservice.eserviceId, client.consumerId, *)
        .once()
        .returns(Future.successful(Seq(agreement)))

      (mockPurposeManagementService
        .getPurpose(_: UUID)(_: Seq[(String, String)]))
        .expects(clientPurpose.states.purpose.purposeId, *)
        .once()
        .returns(Future.successful(purpose.copy(eserviceId = eService.id, consumerId = consumer.id)))

      (mockCatalogManagementService
        .getEService(_: UUID)(_: Seq[(String, String)]))
        .expects(agreement.eserviceId, *)
        .once()
        .returns(
          Future.successful(eService.copy(descriptors = Seq(activeDescriptor.copy(id = agreement.descriptorId))))
        )

      (mockAuthorizationManagementService
        .listClients(
          _: Option[Int],
          _: Option[Int],
          _: Option[UUID],
          _: Option[UUID],
          _: Option[UUID],
          _: Option[authorizationmanagement.client.model.ClientKind]
        )(_: Seq[(String, String)]))
        .expects(
          offset,
          limit,
          relationshipUuid,
          consumerUuid,
          purposeUuid,
          Some(authorizationmanagement.client.model.ClientKind.CONSUMER),
          *
        )
        .once()
        .returns(Future.successful(Seq(client)))

      (mockPartyManagementService
        .getInstitution(_: UUID)(_: Seq[(String, String)], _: ExecutionContext))
        .expects(client.consumerId, *, *)
        .once()
        .returns(Future.successful(consumer))

      val expectedAgreement: Agreement = Agreement(
        id = agreement.id,
        eservice = CatalogManagementService.eServiceToApi(eService),
        descriptor = CatalogManagementService.descriptorToApi(activeDescriptor.copy(id = agreement.descriptorId))
      )

      val expected = Clients(
        List(
          Client(
            id = client.id,
            consumer = Organization(consumer.originId, consumer.description),
            name = client.name,
            purposes =
              client.purposes.map(AuthorizationManagementService.purposeToApi(_, purpose.title, expectedAgreement)),
            description = client.description,
            operators = Some(Seq.empty),
            kind = ClientKind.CONSUMER
          )
        )
      )

      Get() ~> service.listClients(
        offset,
        limit,
        client.consumerId.toString,
        Some(clientPurpose.states.purpose.purposeId.toString),
        Some("CONSUMER")
      ) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Clients] shouldEqual expected
      }
    }
  }

  "Client delete" should {
    "succeed" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(*, *)
        .once()
        .returns(Future.successful(client))

      (mockAuthorizationManagementService
        .deleteClient(_: UUID)(_: Seq[(String, String)]))
        .expects(*, *)
        .once()
        .returns(Future.successful(()))

      Get() ~> service.deleteClient(client.id.toString) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if client does not exist" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(*, *)
        .once()
        .returns(Future.successful(client))

      (mockAuthorizationManagementService
        .deleteClient(_: UUID)(_: Seq[(String, String)]))
        .expects(*, *)
        .once()
        .returns(Future.failed(authorizationmanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.deleteClient(client.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

}
