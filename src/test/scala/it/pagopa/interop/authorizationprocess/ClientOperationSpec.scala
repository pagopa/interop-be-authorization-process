package it.pagopa.interop.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.authorizationmanagement
import it.pagopa.interop.authorizationprocess.api.impl.ClientApiServiceImpl
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationprocess.service.{AuthorizationManagementService, PartyManagementService}
import it.pagopa.interop.authorizationprocess.util.SpecUtils
import it.pagopa.interop.partymanagement.client.model.Relationships
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class ClientOperationSpec extends AnyWordSpecLike with MockFactory with SpecUtils with ScalatestRouteTest {

  import clientApiMarshaller._

  val service: ClientApiServiceImpl = ClientApiServiceImpl(
    mockAuthorizationManagementService,
    mockAgreementManagementService,
    mockCatalogManagementService,
    mockPartyManagementService,
    mockPurposeManagementService,
    mockUserRegistryManagementService,
    mockJwtReader
  )(ExecutionContext.global)

  "Client creation" should {
    "succeed" in {
      (mockJwtReader
        .getClaims(_: String))
        .expects(bearerToken)
        .returning(mockSubject(UUID.randomUUID().toString))
        .once()

      (mockAuthorizationManagementService
        .createClient(_: UUID, _: String, _: Option[String], _: authorizationmanagement.client.model.ClientKind)(
          _: String
        ))
        .expects(
          organization.id,
          clientSeed.name,
          clientSeed.description,
          authorizationmanagement.client.model.ClientKind.CONSUMER,
          bearerToken
        )
        .once()
        .returns(Future.successful(client))

      mockClientComposition(withOperators = false)

      val expected = Client(
        id = client.id,
        consumer = Organization(consumer.institutionId, consumer.description),
        name = client.name,
        purposes = client.purposes.map(AuthorizationManagementService.purposeToApi),
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
      Get() ~> service.createConsumerClient(clientSeed) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

  }

  "Client retrieve" should {
    "succeed" in {
      (mockJwtReader
        .getClaims(_: String))
        .expects(bearerToken)
        .returning(mockSubject(UUID.randomUUID().toString))
        .once()

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: String))
        .expects(*, bearerToken)
        .once()
        .returns(Future.successful(client))

      mockClientComposition(withOperators = false)

      val expected =
        Client(
          id = client.id,
          consumer = Organization(consumer.institutionId, consumer.description),
          name = client.name,
          purposes = client.purposes.map(AuthorizationManagementService.purposeToApi),
          description = client.description,
          operators = Some(Seq.empty),
          kind = ClientKind.CONSUMER
        )

      Get() ~> service.getClient(client.id.toString) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Client] shouldEqual expected
      }
    }

    "fail if client does not exist" in {
      (mockJwtReader
        .getClaims(_: String))
        .expects(bearerToken)
        .returning(mockSubject(UUID.randomUUID().toString))
        .once()

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: String))
        .expects(*, bearerToken)
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
      val purposeUuid: Option[UUID]      = Some(clientPurpose.purposeId)

      (mockJwtReader
        .getClaims(_: String))
        .expects(bearerToken)
        .returning(mockSubject(UUID.randomUUID().toString))
        .once()

      (mockPartyManagementService
        .getRelationships(_: UUID, _: UUID, _: Seq[String])(_: String))
        .expects(
          consumerId,
          personId,
          Seq(PartyManagementService.PRODUCT_ROLE_SECURITY_OPERATOR, PartyManagementService.PRODUCT_ROLE_ADMIN),
          bearerToken
        )
        .once()
        .returns(Future.successful(Relationships(Seq(relationship))))

      (mockAuthorizationManagementService
        .listClients(
          _: Option[Int],
          _: Option[Int],
          _: Option[UUID],
          _: Option[UUID],
          _: Option[UUID],
          _: Option[authorizationmanagement.client.model.ClientKind]
        )(_: String))
        .expects(
          offset,
          limit,
          relationshipUuid,
          consumerUuid,
          purposeUuid,
          Some(authorizationmanagement.client.model.ClientKind.CONSUMER),
          bearerToken
        )
        .once()
        .returns(Future.successful(Seq(client)))

      (mockPartyManagementService
        .getOrganization(_: UUID)(_: String))
        .expects(client.consumerId, bearerToken)
        .once()
        .returns(Future.successful(consumer))

      val expected = Clients(
        List(
          Client(
            id = client.id,
            consumer = Organization(consumer.institutionId, consumer.description),
            name = client.name,
            purposes = client.purposes.map(AuthorizationManagementService.purposeToApi),
            description = client.description,
            operators = Some(Seq.empty),
            kind = ClientKind.CONSUMER
          )
        )
      )

      Get() ~> service.listClients(
        client.consumerId.toString,
        offset,
        limit,
        Some(clientPurpose.purposeId.toString),
        Some("CONSUMER")
      ) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Clients] shouldEqual expected
      }
    }
  }

  "Client delete" should {
    "succeed" in {
      (mockJwtReader
        .getClaims(_: String))
        .expects(bearerToken)
        .returning(mockSubject(UUID.randomUUID().toString))
        .once()

      (mockAuthorizationManagementService
        .deleteClient(_: UUID)(_: String))
        .expects(*, bearerToken)
        .once()
        .returns(Future.successful(()))

      Get() ~> service.deleteClient(client.id.toString) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if client does not exist" in {
      (mockJwtReader
        .getClaims(_: String))
        .expects(bearerToken)
        .returning(mockSubject(UUID.randomUUID().toString))
        .once()

      (mockAuthorizationManagementService
        .deleteClient(_: UUID)(_: String))
        .expects(*, bearerToken)
        .once()
        .returns(Future.failed(authorizationmanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.deleteClient(client.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

}
