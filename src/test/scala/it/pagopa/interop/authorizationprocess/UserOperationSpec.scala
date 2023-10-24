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
import it.pagopa.interop.authorizationprocess.service._
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
          USER_ROLES       -> "admin",
          "organizationId" -> consumerId.toString,
          "uid"            -> userId.toString,
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
        .expects(
          selfcareId,
          consumerId,
          userId.some,
          Seq(SelfcareV2ClientService.PRODUCT_ROLE_SECURITY_USER, SelfcareV2ClientService.PRODUCT_ROLE_ADMIN),
          *,
          *
        )
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
        .expects(
          selfcareId,
          consumerId,
          userId.some,
          Seq(SelfcareV2ClientService.PRODUCT_ROLE_SECURITY_USER, SelfcareV2ClientService.PRODUCT_ROLE_ADMIN),
          *,
          *
        )
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
        .expects(
          selfcareId,
          consumerId,
          userId.some,
          Seq(SelfcareV2ClientService.PRODUCT_ROLE_SECURITY_USER, SelfcareV2ClientService.PRODUCT_ROLE_ADMIN),
          *,
          *
        )
        .once()
        .returns(Future.failed(InstitutionNotFound(selfcareId)))

      Post() ~> service.addUser(persistentClient.id.toString, userId.toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-0022"
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
        .expects(
          selfcareId,
          consumerId,
          userId.some,
          Seq(SelfcareV2ClientService.PRODUCT_ROLE_SECURITY_USER, SelfcareV2ClientService.PRODUCT_ROLE_ADMIN),
          *,
          *
        )
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
        .expects(
          selfcareId,
          consumerId,
          userId.some,
          Seq(SelfcareV2ClientService.PRODUCT_ROLE_SECURITY_USER, SelfcareV2ClientService.PRODUCT_ROLE_ADMIN),
          *,
          *
        )
        .once()
        .returns(Future.successful(results))

      Post() ~> service.addUser(persistentClient.id.toString, userId.toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-0021"
      }
    }
  }

  "User removal" should {
    "succeed" in {

      val results: Seq[UserResource] = Seq(userResource)

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(*, *, *)
        .once()
        .returns(Future.successful(persistentClient))

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: Option[UUID], _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, personId, userId.some, Seq.empty, *, *)
        .once()
        .returns(Future.successful(results))

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

    "succeed if an admin user removes own user" in {

      val results: Seq[UserResource] = Seq(userResource)

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(*, *, *)
        .once()
        .returns(Future.successful(persistentClient))

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: Option[UUID], _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, personId, userId.some, Seq.empty, *, *)
        .once()
        .returns(Future.successful(results))

      (mockAuthorizationManagementService
        .removeUser(_: UUID, _: UUID)(_: Seq[(String, String)]))
        .expects(persistentClient.id, userId, *)
        .once()
        .returns(Future.unit)

      Delete() ~> service.removeUser(persistentClient.id.toString, userId.toString) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if a security user removes own user" in {

      val results: Seq[UserResource] = Seq(userResource.copy(roles = Some(Seq("security"))))

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(*, *, *)
        .once()
        .returns(Future.successful(persistentClient))

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: Option[UUID], _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, personId, userId.some, Seq.empty, *, *)
        .once()
        .returns(Future.successful(results))

      Delete() ~> service.removeUser(persistentClient.id.toString, userId.toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-0004"
      }
    }

    "fail if Institution not found" in {

      val results: Seq[UserResource] = Seq(userResource)

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(*, *, *)
        .once()
        .returns(Future.successful(persistentClient))

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: Option[UUID], _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, personId, userId.some, Seq.empty, *, *)
        .once()
        .returns(Future.successful(results))

      (mockAuthorizationManagementService
        .removeUser(_: UUID, _: UUID)(_: Seq[(String, String)]))
        .expects(persistentClient.id, userId, *)
        .once()
        .returns(Future.failed(InstitutionNotFound(selfcareId)))

      Delete() ~> service.removeUser(persistentClient.id.toString, userId.toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-0022"
      }
    }
    "fail if User not found" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(*, *, *)
        .once()
        .returns(Future.successful(persistentClient))

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: Option[UUID], _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, personId, userId.some, Seq.empty, *, *)
        .once()
        .returns(Future.successful(Seq.empty))

      Delete() ~> service.removeUser(persistentClient.id.toString, userId.toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-0023"
      }
    }
    "fail if User has empty fields" in {

      val results: Seq[UserResource] = Seq(emptyUserResource)

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(*, *, *)
        .once()
        .returns(Future.successful(persistentClient))

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: Option[UUID], _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, personId, userId.some, Seq.empty, *, *)
        .once()
        .returns(Future.successful(results))

      Delete() ~> service.removeUser(persistentClient.id.toString, userId.toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-0021"
      }
    }
  }

  "User retrieve" should {
    "succeed" in {
      val results: Seq[UserResource] = Seq(userResource)

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient.copy(users = Set(userId))))

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: Option[UUID], _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, personId, userId.some, Seq.empty, *, *)
        .once()
        .returns(Future.successful(results))

      (mockSelfcareV2ClientService
        .getUserById(_: UUID, _: UUID)(_: Seq[(String, String)], _: ExecutionContext))
        .expects(selfcareId, userId, *, *)
        .once()
        .returns(Future.successful(userResponse))

      val expected =
        Seq(User(userId = userId, taxCode = taxCode, name = "name", familyName = "surname", roles = Seq("admin")))

      Get() ~> service.getClientUsers(persistentClient.id.toString) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Seq[User]] shouldEqual expected
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

    "fail if Institution not found" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient.copy(users = Set(userId))))

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: Option[UUID], _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, personId, userId.some, Seq.empty, *, *)
        .once()
        .returns(Future.failed(InstitutionNotFound(selfcareId)))

      Get() ~> service.getClientUsers(persistentClient.id.toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-0022"
      }
    }
    "fail if User not found" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient.copy(users = Set(userId))))

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: Option[UUID], _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, personId, userId.some, Seq.empty, *, *)
        .once()
        .returns(Future.successful(Seq.empty))

      Get() ~> service.getClientUsers(persistentClient.id.toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-0023"
      }
    }
    "fail if User has empty fields" in {

      val results: Seq[UserResource] = Seq(emptyUserResource)

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient.copy(users = Set(userId))))

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: Option[UUID], _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, personId, userId.some, Seq.empty, *, *)
        .once()
        .returns(Future.successful(results))

      Get() ~> service.getClientUsers(persistentClient.id.toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-0021"
      }
    }
  }

  "User retrieve keys" should {
    "succeed" in {

      val results: Seq[UserResource] = Seq(userResource)

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient.copy(users = Set(userId))))

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: Option[UUID], _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, personId, userId.some, Seq.empty, *, *)
        .once()
        .returns(Future.successful(results))

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

    "fail if Institution not found" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient.copy(users = Set(userId))))

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: Option[UUID], _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, personId, userId.some, Seq.empty, *, *)
        .once()
        .returns(Future.failed(InstitutionNotFound(selfcareId)))

      Get() ~> serviceUser.getClientUserKeys(persistentClient.id.toString, userId.toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-0022"
      }
    }
    "fail if User not found" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient.copy(users = Set(userId))))

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: Option[UUID], _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, personId, userId.some, Seq.empty, *, *)
        .once()
        .returns(Future.successful(Seq.empty))

      Get() ~> serviceUser.getClientUserKeys(persistentClient.id.toString, userId.toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-0023"
      }
    }
    "fail if User has empty fields" in {

      val results: Seq[UserResource] = Seq(emptyUserResource)

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient.copy(users = Set(userId))))

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: Option[UUID], _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, personId, userId.some, Seq.empty, *, *)
        .once()
        .returns(Future.successful(results))

      Get() ~> serviceUser.getClientUserKeys(persistentClient.id.toString, userId.toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-0021"
      }
    }
  }
}
