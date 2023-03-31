package it.pagopa.interop.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.authorizationmanagement
import it.pagopa.interop.authorizationprocess.common.readmodel.TotalCountResult
import org.mongodb.scala.bson.conversions.Bson
import spray.json.JsonReader
import it.pagopa.interop.authorizationmanagement.client.api.{ClientApi, KeyApi, PurposeApi}
import it.pagopa.interop.authorizationmanagement.model.client.{PersistentClient, Api}
import it.pagopa.interop.authorizationprocess.api.impl.ClientApiServiceImpl
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors.{ClientNotFound, PurposeNotFound}
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationprocess.service.impl.AuthorizationManagementServiceImpl
import it.pagopa.interop.authorizationprocess.service.AuthorizationManagementInvoker
import it.pagopa.interop.authorizationprocess.util.SpecUtilsWithImplicit
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike
import it.pagopa.interop.commons.utils.USER_ROLES

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
    mockUserRegistryManagementService,
    mockTenantManagementService,
    mockReadModel
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

      val expected = Client(
        id = client.id,
        consumerId = consumerId,
        name = client.name,
        purposes = Seq(clientPurposeProcess),
        relationshipsIds = Set.empty,
        description = client.description,
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
        mockUserRegistryManagementService,
        mockTenantManagementService,
        mockReadModel
      )(ExecutionContext.global)
      Get() ~> service.createConsumerClient(clientSeed) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }
  }

  "Client retrieve" should {
    "succeed in case of requester is the consumer" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(*, *)
        .once()
        .returns(Future.successful(client))

      val expected =
        Client(
          id = client.id,
          consumerId = consumerId,
          name = client.name,
          purposes = Seq(clientPurposeProcess),
          description = client.description,
          relationshipsIds = Set.empty,
          kind = ClientKind.CONSUMER
        )

      Get() ~> service.getClient(client.id.toString) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Client] shouldEqual expected
      }
    }

    "succeed in case of requester is the producer" in {
      implicit val contexts: Seq[(String, String)] =
        Seq(
          "bearer"         -> bearerToken,
          "uid"            -> personId.toString,
          USER_ROLES       -> "admin",
          "organizationId" -> eService.producerId.toString
        )
      val anotherConsumerId                        = UUID.randomUUID()
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(*, *)
        .once()
        .returns(Future.successful(client.copy(consumerId = anotherConsumerId)))

      (mockCatalogManagementService
        .getEService(_: UUID)(_: Seq[(String, String)]))
        .expects(*, *)
        .once()
        .returns(Future.successful(eService))

      val expected =
        Client(
          id = client.id,
          consumerId = anotherConsumerId,
          name = client.name,
          purposes = Seq(clientPurposeProcess),
          description = client.description,
          relationshipsIds = Set.empty,
          kind = ClientKind.CONSUMER
        )

      Get() ~> service.getClient(client.id.toString) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Client] shouldEqual expected
      }
    }

    "fail if the organization in the token is not the same" in {
      val anotherConsumerId = UUID.randomUUID()
      val anotherProducerId = UUID.randomUUID()

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(*, *)
        .once()
        .returns(Future.successful(client.copy(consumerId = anotherConsumerId)))

      (mockCatalogManagementService
        .getEService(_: UUID)(_: Seq[(String, String)]))
        .expects(*, *)
        .once()
        .returns(Future.successful(eService.copy(producerId = anotherProducerId)))

      Get() ~> service
        .getClient(client.id.toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
        entityAs[Problem].errors.head.code shouldBe "007-0008"
      }
    }

    "fail if client does not exist" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(*, *)
        .once()
        .returns(Future.failed(ClientNotFound(client.id)))

      Get() ~> service.getClient(client.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Client list" should {
    "succeed with ADMIN role" in {
      val consumerId = UUID.randomUUID()
      val client     = PersistentClient(
        id = UUID.randomUUID(),
        consumerId = consumerId,
        name = "name",
        purposes = Seq.empty,
        description = None,
        relationships = Set.empty,
        kind = Api
      )

      val clients: Seq[PersistentClient] = Seq(client)

      val offset: Int = 0
      val limit: Int  = 50

      (mockReadModel
        .aggregate(_: String, _: Seq[Bson], _: Int, _: Int)(_: JsonReader[_], _: ExecutionContext))
        .expects("clients", *, offset, limit, *, *)
        .once()
        .returns(Future.successful(clients))

      (mockReadModel
        .aggregate(_: String, _: Seq[Bson], _: Int, _: Int)(_: JsonReader[_], _: ExecutionContext))
        .expects("clients", *, offset, Int.MaxValue, *, *)
        .once()
        .returns(Future.successful(Seq(TotalCountResult(1))))

      Get() ~> service.getClients(
        Option("name"),
        UUID.randomUUID().toString,
        consumerId.toString,
        Some(UUID.randomUUID().toString),
        None,
        offset,
        limit
      ) ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
    "succeed with SECURITY role" in {

      val consumerId = UUID.randomUUID()

      implicit val contexts: Seq[(String, String)] =
        Seq(
          "bearer"         -> bearerToken,
          "uid"            -> personId.toString,
          USER_ROLES       -> "security",
          "organizationId" -> consumerId.toString
        )

      val client = PersistentClient(
        id = UUID.randomUUID(),
        consumerId = consumerId,
        name = "name",
        purposes = Seq.empty,
        description = None,
        relationships = Set.empty,
        kind = Api
      )

      val clients: Seq[PersistentClient] = Seq(client)

      val offset: Int = 0
      val limit: Int  = 50

      (mockTenantManagementService
        .getTenant(_: UUID)(_: Seq[(String, String)]))
        .expects(*, *)
        .once()
        .returns(Future.successful(consumer))

      (mockPartyManagementService
        .getRelationships(_: String, _: UUID, _: Seq[String])(_: Seq[(String, String)], _: ExecutionContext))
        .expects(consumer.selfcareId.get, *, *, *, *)
        .once()
        .returns(Future.successful(relationships))

      (mockReadModel
        .aggregate(_: String, _: Seq[Bson], _: Int, _: Int)(_: JsonReader[_], _: ExecutionContext))
        .expects("clients", *, offset, limit, *, *)
        .once()
        .returns(Future.successful(clients))

      (mockReadModel
        .aggregate(_: String, _: Seq[Bson], _: Int, _: Int)(_: JsonReader[_], _: ExecutionContext))
        .expects("clients", *, offset, Int.MaxValue, *, *)
        .once()
        .returns(Future.successful(Seq(TotalCountResult(1))))

      Get() ~> service.getClients(
        Option("name"),
        UUID.randomUUID().toString,
        consumerId.toString,
        Some(UUID.randomUUID().toString),
        None,
        offset,
        limit
      ) ~> check {
        status shouldEqual StatusCodes.OK
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
        .returns(Future.failed(ClientNotFound(client.id)))

      Get() ~> service.deleteClient(client.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Client purpose in archived state delete" should {
    "succeed" in {
      val clients: Seq[PersistentClient] = Seq(persistentClient)

      val offset: Int = 0
      val limit: Int  = Int.MaxValue

      (mockPurposeManagementService
        .getPurpose(_: UUID)(_: Seq[(String, String)]))
        .expects(purpose.id, *)
        .once()
        .returns(Future.successful(archivedPurpose))

      (mockReadModel
        .find(_: String, _: Bson, _: Int, _: Int)(_: JsonReader[_], _: ExecutionContext))
        .expects("clients", *, offset, limit, *, *)
        .once()
        .returns(Future.successful(clients))

      (mockAuthorizationManagementService
        .removeClientPurpose(_: UUID, _: UUID)(_: Seq[(String, String)]))
        .expects(*, *, *)
        .once()
        .returns(Future.successful(()))

      Get() ~> service.removeArchivedPurpose(purpose.id.toString) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if purpose does not exist" in {
      (mockPurposeManagementService
        .getPurpose(_: UUID)(_: Seq[(String, String)]))
        .expects(purpose.id, *)
        .once()
        .returns(Future.failed(PurposeNotFound(purpose.id)))

      Get() ~> service.removeArchivedPurpose(purpose.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
        entityAs[Problem].errors.head.code shouldBe "007-0014"
      }
    }

    "fail if purpose version is not in archived state" in {
      (mockPurposeManagementService
        .getPurpose(_: UUID)(_: Seq[(String, String)]))
        .expects(purpose.id, *)
        .once()
        .returns(Future.successful(notArchivedPurpose))

      Get() ~> service.removeArchivedPurpose(purpose.id.toString) ~> check {
        status shouldEqual StatusCodes.BadRequest
        entityAs[Problem].errors.head.code shouldBe "007-0015"
      }
    }
  }

}
