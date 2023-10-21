package it.pagopa.interop.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.authorizationmanagement
import it.pagopa.interop.authorizationmanagement.model.client.{Api, PersistentClient, PersistentClientKind}
import it.pagopa.interop.authorizationprocess.api.impl.ClientApiServiceImpl
import it.pagopa.interop.authorizationprocess.common.readmodel.PaginatedResult
import it.pagopa.interop.authorizationprocess.common.readmodel.model.ReadModelClientWithKeys
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors.{InstitutionNotFound, ClientNotFound}
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationprocess.util.SpecUtilsWithImplicit
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.commons.utils.USER_ROLES
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import it.pagopa.interop.selfcare.v2.client.model.UserResource

class ClientOperationSpec extends AnyWordSpecLike with MockFactory with SpecUtilsWithImplicit with ScalatestRouteTest {

  import clientApiMarshaller._

  val service: ClientApiServiceImpl = ClientApiServiceImpl(
    mockAuthorizationManagementService,
    mockAgreementManagementService,
    mockCatalogManagementService,
    mockSelfcareV2ClientService,
    mockPurposeManagementService,
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
        users = Set.empty,
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
      Get() ~> service.createConsumerClient(clientSeed) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-9989"
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
          users = Set.empty,
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
        Seq("bearer" -> bearerToken, USER_ROLES -> "admin", "organizationId" -> UUID.randomUUID().toString)
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
          users = Set.empty,
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
        responseAs[Problem].errors.head.code shouldEqual "007-0010"
      }
    }
  }

  "Client list" should {
    "succeed with ADMIN role" in {

      val client = PersistentClient(
        id = UUID.randomUUID(),
        consumerId = consumerId,
        name = "name",
        purposes = Seq.empty,
        description = None,
        relationships = Set.empty,
        users = Set.empty,
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

      val securityRole = "security"
      val roles        = Seq(securityRole)

      implicit val contexts: Seq[(String, String)] =
        Seq(
          "bearer"         -> bearerToken,
          USER_ROLES       -> securityRole,
          "organizationId" -> consumerId.toString,
          "uid"            -> personId.toString,
          "selfcareId"     -> selfcareId.toString
        )

      val client = PersistentClient(
        id = UUID.randomUUID(),
        consumerId = consumerId,
        name = "name",
        purposes = Seq.empty,
        description = None,
        relationships = Set.empty,
        users = Set.empty,
        kind = Api,
        createdAt = timestamp
      )

      val clients: Seq[PersistentClient] = Seq(client)

      val offset: Int = 0
      val limit: Int  = 50

      val results: Seq[UserResource] = Seq(userResource)

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: UUID, _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, consumerId, personId, roles, *, *)
        .once()
        .returns(Future.successful(results))

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
    "fail with SECURITY role and Institution Not Found" in {
      val securityRole = "security"
      val roles        = Seq(securityRole)

      implicit val contexts: Seq[(String, String)] =
        Seq(
          "bearer"         -> bearerToken,
          USER_ROLES       -> securityRole,
          "organizationId" -> consumerId.toString,
          "uid"            -> personId.toString,
          "selfcareId"     -> selfcareId.toString
        )

      val offset: Int = 0
      val limit: Int  = 50

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: UUID, _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, consumerId, personId, roles, *, *)
        .once()
        .returns(Future.failed(InstitutionNotFound(selfcareId)))

      Get() ~> service.getClients(
        Option("name"),
        UUID.randomUUID().toString,
        consumerId.toString,
        Some(UUID.randomUUID().toString),
        None,
        offset,
        limit
      ) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-0022"
      }
    }
    "fail with SECURITY role and Users not Found" in {
      val securityRole = "security"
      val roles        = Seq(securityRole)

      implicit val contexts: Seq[(String, String)] =
        Seq(
          "bearer"         -> bearerToken,
          USER_ROLES       -> securityRole,
          "organizationId" -> consumerId.toString,
          "uid"            -> personId.toString,
          "selfcareId"     -> selfcareId.toString
        )

      val offset: Int = 0
      val limit: Int  = 50

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: UUID, _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, consumerId, personId, roles, *, *)
        .once()
        .returns(Future.successful(Seq.empty))

      Get() ~> service.getClients(
        Option("name"),
        UUID.randomUUID().toString,
        consumerId.toString,
        Some(UUID.randomUUID().toString),
        None,
        offset,
        limit
      ) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-0003"
      }
    }
    "fail with SECURITY role and User has empty fields" in {
      val securityRole = "security"
      val roles        = Seq(securityRole)

      implicit val contexts: Seq[(String, String)] =
        Seq(
          "bearer"         -> bearerToken,
          USER_ROLES       -> securityRole,
          "organizationId" -> consumerId.toString,
          "uid"            -> personId.toString,
          "selfcareId"     -> selfcareId.toString
        )

      val offset: Int = 0
      val limit: Int  = 50

      val results: Seq[UserResource] = Seq(emptyUserResource)

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: UUID, _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, consumerId, personId, roles, *, *)
        .once()
        .returns(Future.successful(results))

      Get() ~> service.getClients(
        Option("name"),
        UUID.randomUUID().toString,
        consumerId.toString,
        Some(UUID.randomUUID().toString),
        None,
        offset,
        limit
      ) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-0021"
      }
    }
  }
  "Client Keys list" should {
    "succeed with ADMIN role" in {

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
          users = Set(userId),
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
        userId.toString,
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
      val securityRole = "security"
      val roles        = Seq(securityRole)

      implicit val contexts: Seq[(String, String)] =
        Seq(
          "bearer"         -> bearerToken,
          USER_ROLES       -> securityRole,
          "organizationId" -> consumerId.toString,
          "uid"            -> personId.toString,
          "selfcareId"     -> selfcareId.toString
        )

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
          users = Set(userId),
          kind = Api,
          keys = Seq.empty
        )
      )

      val results: Seq[UserResource] = Seq(userResource)

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: UUID, _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, consumerId, personId, roles, *, *)
        .once()
        .returns(Future.successful(results))

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
        userId.toString,
        consumerId.toString,
        None,
        None,
        offset,
        limit
      ) ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
    "fail with SECURITY role and Institution Not Found" in {
      val securityRole = "security"
      val roles        = Seq(securityRole)

      implicit val contexts: Seq[(String, String)] =
        Seq(
          "bearer"         -> bearerToken,
          USER_ROLES       -> securityRole,
          "organizationId" -> consumerId.toString,
          "uid"            -> personId.toString,
          "selfcareId"     -> selfcareId.toString
        )

      val offset: Int = 0
      val limit: Int  = 50

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: UUID, _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, consumerId, personId, roles, *, *)
        .once()
        .returns(Future.failed(InstitutionNotFound(selfcareId)))

      Get() ~> service.getClientsWithKeys(
        Option("name"),
        userId.toString,
        consumerId.toString,
        None,
        None,
        offset,
        limit
      ) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-0022"
      }
    }
    "fail with SECURITY role and Users not Found" in {
      val securityRole = "security"
      val roles        = Seq(securityRole)

      implicit val contexts: Seq[(String, String)] =
        Seq(
          "bearer"         -> bearerToken,
          USER_ROLES       -> securityRole,
          "organizationId" -> consumerId.toString,
          "uid"            -> personId.toString,
          "selfcareId"     -> selfcareId.toString
        )

      val offset: Int = 0
      val limit: Int  = 50

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: UUID, _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, consumerId, personId, roles, *, *)
        .once()
        .returns(Future.successful(Seq.empty))

      Get() ~> service.getClientsWithKeys(
        Option("name"),
        userId.toString,
        consumerId.toString,
        None,
        None,
        offset,
        limit
      ) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-0003"
      }
    }
    "fail with SECURITY role and User is empty" in {
      val securityRole = "security"
      val roles        = Seq(securityRole)

      implicit val contexts: Seq[(String, String)] =
        Seq(
          "bearer"         -> bearerToken,
          USER_ROLES       -> securityRole,
          "organizationId" -> consumerId.toString,
          "uid"            -> userId.toString,
          "selfcareId"     -> selfcareId.toString
        )

      val offset: Int = 0
      val limit: Int  = 50

      val results: Seq[UserResource] = Seq(emptyUserResource)

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: UUID, _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, consumerId, userId, roles, *, *)
        .once()
        .returns(Future.successful(results))

      Get() ~> service.getClientsWithKeys(
        Option("name"),
        userId.toString,
        consumerId.toString,
        None,
        None,
        offset,
        limit
      ) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-0021"
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
        responseAs[Problem].errors.head.code shouldEqual "007-0010"
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
