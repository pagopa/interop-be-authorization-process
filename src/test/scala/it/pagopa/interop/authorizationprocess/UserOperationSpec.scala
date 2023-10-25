package it.pagopa.interop.authorizationprocess

import cats.syntax.all._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.authorizationprocess.api.impl.{ClientApiServiceImpl, UserApiServiceImpl}
import it.pagopa.interop.authorizationprocess.api.impl.ClientApiMarshallerImpl._
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors.{InstitutionNotFound, ClientNotFound}
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.commons.utils.USER_ROLES
import it.pagopa.interop.selfcare.v2.client.model.UserResource
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.commons.jwt.{ADMIN_ROLE, SECURITY_ROLE}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike
import it.pagopa.interop.authorizationprocess.util._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class UserOperationSpec extends AnyWordSpecLike with MockFactory with SpecUtilsWithImplicit with ScalatestRouteTest {

  val serviceUser: UserApiServiceImpl =
    UserApiServiceImpl(mockAuthorizationManagementService, mockSelfcareV2ClientService)(
      ExecutionContext.global,
      mockReadModel
    )

  val service: ClientApiServiceImpl = ClientApiServiceImpl(
    mockAuthorizationManagementService,
    mockAgreementManagementService,
    mockCatalogManagementService,
    mockSelfcareV2ClientService,
    mockPurposeManagementService,
    mockTenantManagementService,
    mockDateTimeSupplier
  )(ExecutionContext.global, mockReadModel)

  "User addition" should {
    "succeed on existing user" in {
      implicit val contexts: Seq[(String, String)] =
        Seq(
          "bearer"         -> bearerToken,
          USER_ROLES       -> ADMIN_ROLE,
          "organizationId" -> consumerId.toString,
          "uid"            -> personId.toString,
          "selfcareId"     -> selfcareId.toString
        )

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient))

      val results: Seq[UserResource] = Seq(userResource)

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: Option[UUID], _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, personId, userId.some, Seq(SECURITY_ROLE, ADMIN_ROLE), *, *)
        .once()
        .returns(Future.successful(results))

      (mockAuthorizationManagementService
        .addUser(_: UUID, _: UUID)(_: Seq[(String, String)]))
        .expects(persistentClient.id, userId, *)
        .once()
        .returns(Future.successful(client.copy(id = persistentClient.id, users = Set(userId))))

      val expected = Client(
        id = persistentClient.id,
        consumerId = consumerId,
        name = client.name,
        purposes = Seq(clientPurposeProcess),
        description = client.description,
        users = Set(userId),
        kind = ClientKind.CONSUMER,
        createdAt = timestamp
      )

      Post() ~> service.addUser(persistentClient.id.toString, userId.toString) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Client] shouldEqual expected
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]

      Get() ~> service.addUser(client.id.toString, userId.toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }

    "fail if client does not exist" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.failed(ClientNotFound(persistentClient.id)))

      Get() ~> service.addUser(persistentClient.id.toString, userId.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
        responseAs[Problem].errors.head.code shouldEqual "007-0010"
      }
    }

    "fail if user is already assigned" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient.copy(users = Set(userId))))

      val results: Seq[UserResource] = Seq(userResource)

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: Option[UUID], _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, personId, userId.some, Seq(SECURITY_ROLE, ADMIN_ROLE), *, *)
        .once()
        .returns(Future.successful(results))

      Post() ~> service.addUser(persistentClient.id.toString, userId.toString) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[Problem].errors.head.code shouldEqual "007-0001"
      }
    }

    "fail when Institution Not Found" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient))

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: Option[UUID], _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, personId, userId.some, Seq(SECURITY_ROLE, ADMIN_ROLE), *, *)
        .once()
        .returns(Future.failed(InstitutionNotFound(selfcareId)))

      Post() ~> service.addUser(persistentClient.id.toString, userId.toString) ~> check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[Problem].errors.head.code shouldEqual "007-9991"
      }
    }
    "fail when Security User not Found" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient))

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: Option[UUID], _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, personId, userId.some, Seq(SECURITY_ROLE, ADMIN_ROLE), *, *)
        .once()
        .returns(Future.successful(Seq.empty))

      Post() ~> service.addUser(persistentClient.id.toString, userId.toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-0003"
      }
    }
    "fail when User is empty" in {

      val results: Seq[UserResource] = Seq(emptyUserResource)

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient))

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: Option[UUID], _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, personId, userId.some, Seq(SECURITY_ROLE, ADMIN_ROLE), *, *)
        .once()
        .returns(Future.successful(results))

      Post() ~> service.addUser(persistentClient.id.toString, userId.toString) ~> check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[Problem].errors.head.code shouldEqual "007-9991"
      }
    }
  }

  "User removal" should {
    "succeed" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(*, *, *)
        .once()
        .returns(Future.successful(persistentClient))

      (mockAuthorizationManagementService
        .removeUser(_: UUID, _: UUID)(_: Seq[(String, String)]))
        .expects(persistentClient.id, userId, *)
        .once()
        .returns(Future.unit)

      Delete() ~> service.removeUser(persistentClient.id.toString, userId.toString) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]

      Delete() ~> service.removeUser(client.id.toString, userId.toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-9989"
      }
    }
  }

  "User retrieve" should {
    "succeed" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient.copy(users = Set(userId))))

      val expected = Seq(userId)

      Get() ~> service.getClientUsers(persistentClient.id.toString) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Seq[UUID]] shouldEqual expected
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]

      Get() ~> service.getClientUsers(client.id.toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-9989"
      }
    }

    "fail if client does not exist" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.failed(ClientNotFound(persistentClient.id)))

      Get() ~> service.getClientUsers(persistentClient.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
        responseAs[Problem].errors.head.code shouldEqual "007-0010"
      }
    }
  }

  "User retrieve keys" should {
    "succeed" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient.copy(users = Set(userId))))

      (mockAuthorizationManagementService
        .getClientKeys(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(Seq(persistentKey)))

      Get() ~> serviceUser.getClientUserKeys(persistentClient.id.toString, userId.toString) ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "fail if the caller is not the client consumer" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient.copy(consumerId = UUID.randomUUID())))

      Get() ~> serviceUser.getClientUserKeys(persistentClient.id.toString, UUID.randomUUID().toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-0008"
      }
    }

    "fail if client does not exist" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.failed(ClientNotFound(persistentClient.id)))

      Get() ~> serviceUser.getClientUserKeys(persistentClient.id.toString, userId.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
        responseAs[Problem].errors.head.code shouldEqual "007-0010"
      }
    }

  }
}
