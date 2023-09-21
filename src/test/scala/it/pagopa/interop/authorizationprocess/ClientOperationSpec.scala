package it.pagopa.interop.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.authorizationmanagement
import it.pagopa.interop.authorizationmanagement.client.api.{ClientApi, KeyApi, PurposeApi}
import it.pagopa.interop.authorizationmanagement.model.client.{Api, PersistentClient, PersistentClientKind}
import it.pagopa.interop.authorizationprocess.api.impl.ClientApiServiceImpl
import it.pagopa.interop.authorizationprocess.common.readmodel.PaginatedResult
import it.pagopa.interop.authorizationprocess.common.readmodel.model.ReadModelClientWithKeys
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors.ClientNotFound
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationprocess.service.AuthorizationManagementInvoker
import it.pagopa.interop.authorizationprocess.service.impl.AuthorizationManagementServiceImpl
import it.pagopa.interop.authorizationprocess.util.SpecUtilsWithImplicit
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.commons.utils.USER_ROLES
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.OffsetDateTime
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
    mockDateTimeSupplier
  )(ExecutionContext.global, mockReadModel)

  "Client creation" should {
    "succeed" in {

      (() => service.dateTimeSupplier.get()).expects().returning(timestamp).once()

      (mockAuthorizationManagementService
        .createClient(
          _: UUID,
          _: String,
          _: Option[String],
          _: authorizationmanagement.client.model.ClientKind,
          _: OffsetDateTime,
          _: Seq[UUID]
        )(_: Seq[(String, String)]))
        .expects(
          consumerId,
          clientSeed.name,
          clientSeed.description,
          authorizationmanagement.client.model.ClientKind.CONSUMER,
          timestamp,
          clientSeed.members,
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
        kind = ClientKind.CONSUMER,
        createdAt = timestamp
      )

      Get() ~> service.createConsumerClient(clientSeed) ~> check {
        status shouldEqual StatusCodes.OK
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
        mockDateTimeSupplier
      )(ExecutionContext.global, mockReadModel)
      Get() ~> service.createConsumerClient(clientSeed) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }
  }

  "Client retrieve" should {
    "succeed in case of requester is the consumer" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient.copy(consumerId = consumerId)))

      val expected =
        Client(
          id = persistentClient.id,
          consumerId = consumerId,
          name = persistentClient.name,
          purposes = Seq(clientPurposeProcess),
          description = persistentClient.description,
          relationshipsIds = Set.empty,
          kind = ClientKind.CONSUMER,
          createdAt = timestamp
        )

      Get() ~> service.getClient(persistentClient.id.toString) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Client] shouldEqual expected
      }
    }

    "succeed in case of requester is not the producer" in {
      implicit val contexts: Seq[(String, String)] =
        Seq(
          "bearer"         -> bearerToken,
          "uid"            -> personId.toString,
          USER_ROLES       -> "admin",
          "organizationId" -> UUID.randomUUID().toString
        )
      val anotherConsumerId                        = UUID.randomUUID()
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient.copy(consumerId = anotherConsumerId)))

      val expected =
        Client(
          id = persistentClient.id,
          consumerId = anotherConsumerId,
          name = persistentClient.name,
          purposes = Seq(clientPurposeProcess),
          description = persistentClient.description,
          relationshipsIds = Set.empty,
          kind = ClientKind.CONSUMER,
          createdAt = timestamp
        )

      Get() ~> service.getClient(persistentClient.id.toString) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Client] shouldEqual expected
      }
    }

    "fail if client does not exist" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(*, *, *)
        .once()
        .returns(Future.failed(ClientNotFound(persistentClient.id)))

      Get() ~> service.getClient(persistentClient.id.toString) ~> check {
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
        kind = Api,
        createdAt = timestamp
      )

      val clients: Seq[PersistentClient] = Seq(client)

      val offset: Int = 0
      val limit: Int  = 50

      (mockAuthorizationManagementService
        .getClients(
          _: Option[String],
          _: List[UUID],
          _: UUID,
          _: Option[UUID],
          _: Option[PersistentClientKind],
          _: Int,
          _: Int
        )(_: ExecutionContext, _: ReadModelService))
        .expects(*, *, *, *, *, *, *, *, *)
        .once()
        .returns(Future.successful(PaginatedResult(results = clients, totalCount = clients.size)))

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
        kind = Api,
        createdAt = timestamp
      )

      val clients: Seq[PersistentClient] = Seq(client)

      val offset: Int = 0
      val limit: Int  = 50

      (mockTenantManagementService
        .getTenantById(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(*, *, *)
        .once()
        .returns(Future.successful(consumer))

      (mockPartyManagementService
        .getRelationships(_: String, _: UUID, _: Seq[String])(_: Seq[(String, String)], _: ExecutionContext))
        .expects(consumer.selfcareId.get, *, *, *, *)
        .once()
        .returns(Future.successful(relationships))

      (mockAuthorizationManagementService
        .getClients(
          _: Option[String],
          _: List[UUID],
          _: UUID,
          _: Option[UUID],
          _: Option[PersistentClientKind],
          _: Int,
          _: Int
        )(_: ExecutionContext, _: ReadModelService))
        .expects(*, *, *, *, *, *, *, *, *)
        .once()
        .returns(Future.successful(PaginatedResult(results = clients, totalCount = clients.size)))

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
  "Client Keys list" should {
    "succeed with ADMIN role" in {
      val consumerId     = UUID.randomUUID()
      val clientId       = UUID.randomUUID()
      val relationshipId = UUID.randomUUID()

      val offset: Int = 0
      val limit: Int  = 50

      val keys: Seq[ReadModelClientWithKeys] = Seq(
        ReadModelClientWithKeys(
          id = clientId,
          consumerId = consumerId,
          createdAt = OffsetDateTime.now(),
          name = "name",
          purposes = Seq.empty,
          description = None,
          relationships = Set(relationshipId),
          kind = Api,
          keys = Seq.empty
        )
      )
      (mockAuthorizationManagementService
        .getClientsWithKeys(
          _: Option[String],
          _: List[UUID],
          _: UUID,
          _: Option[UUID],
          _: Option[PersistentClientKind],
          _: Int,
          _: Int
        )(_: ExecutionContext, _: ReadModelService))
        .expects(*, *, *, *, *, *, *, *, *)
        .once()
        .returns(Future.successful(PaginatedResult(results = keys, totalCount = keys.size)))

      Get() ~> service.getClientsWithKeys(
        Option("name"),
        relationshipId.toString,
        consumerId.toString,
        None,
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

      val clientId       = UUID.randomUUID()
      val relationshipId = UUID.randomUUID()

      val offset: Int                        = 0
      val limit: Int                         = 50
      val keys: Seq[ReadModelClientWithKeys] = Seq(
        ReadModelClientWithKeys(
          id = clientId,
          consumerId = consumerId,
          createdAt = OffsetDateTime.now(),
          name = "name",
          purposes = Seq.empty,
          description = None,
          relationships = Set(relationshipId),
          kind = Api,
          keys = Seq.empty
        )
      )

      (mockTenantManagementService
        .getTenantById(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(*, *, *)
        .once()
        .returns(Future.successful(consumer))

      (mockPartyManagementService
        .getRelationships(_: String, _: UUID, _: Seq[String])(_: Seq[(String, String)], _: ExecutionContext))
        .expects(consumer.selfcareId.get, *, *, *, *)
        .once()
        .returns(Future.successful(relationships))

      (mockAuthorizationManagementService
        .getClientsWithKeys(
          _: Option[String],
          _: List[UUID],
          _: UUID,
          _: Option[UUID],
          _: Option[PersistentClientKind],
          _: Int,
          _: Int
        )(_: ExecutionContext, _: ReadModelService))
        .expects(*, *, *, *, *, *, *, *, *)
        .once()
        .returns(Future.successful(PaginatedResult(results = keys, totalCount = keys.size)))

      Get() ~> service.getClientsWithKeys(
        Option("name"),
        relationshipId.toString,
        consumerId.toString,
        None,
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
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(*, *, *)
        .once()
        .returns(Future.successful(persistentClient))

      (mockAuthorizationManagementService
        .deleteClient(_: UUID)(_: Seq[(String, String)]))
        .expects(*, *)
        .once()
        .returns(Future.successful(()))

      Get() ~> service.deleteClient(persistentClient.id.toString) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if client does not exist" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(*, *, *)
        .once()
        .returns(Future.successful(persistentClient))

      (mockAuthorizationManagementService
        .deleteClient(_: UUID)(_: Seq[(String, String)]))
        .expects(*, *)
        .once()
        .returns(Future.failed(ClientNotFound(persistentClient.id)))

      Get() ~> service.deleteClient(persistentClient.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Client purpose delete" should {
    "succeed" in {
      val clients: Seq[PersistentClient] = Seq(persistentClient)

      (mockAuthorizationManagementService
        .getClientsByPurpose(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(*, *, *)
        .once()
        .returns(Future.successful(clients))

      (mockAuthorizationManagementService
        .removeClientPurpose(_: UUID, _: UUID)(_: Seq[(String, String)]))
        .expects(*, *, *)
        .once()
        .returns(Future.successful(()))

      Get() ~> service.removePurposeFromClients(purpose.id.toString) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

  }

}
