package it.pagopa.interop.authorizationprocess

import cats.syntax.all._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.authorizationmanagement.client.{model => AuthorizationManagementDependency}
import it.pagopa.interop.authorizationprocess.api.impl.ClientApiMarshallerImpl._
import it.pagopa.interop.authorizationprocess.api.impl.{ClientApiServiceImpl, keyFormat, keysFormat}
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors.{ClientKeyNotFound, ClientNotFound}
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.selfcare.v2.client.model.UserResource
import it.pagopa.interop.commons.utils.USER_ROLES
import it.pagopa.interop.commons.jwt.{ADMIN_ROLE, SECURITY_ROLE}
import it.pagopa.interop.authorizationprocess.util.{CustomMatchers, SpecUtilsWithImplicit}
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class KeyOperationSpec
    extends AnyWordSpecLike
    with MockFactory
    with SpecUtilsWithImplicit
    with ScalatestRouteTest
    with CustomMatchers {

  val service: ClientApiServiceImpl = ClientApiServiceImpl(
    mockAuthorizationManagementService,
    mockAgreementManagementService,
    mockCatalogManagementService,
    mockSelfcareV2ClientService,
    mockPurposeManagementService,
    mockTenantManagementService,
    mockDateTimeSupplier
  )(ExecutionContext.global, mockReadModel)

  "Retrieve key" should {
    "succeed" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient))

      (mockAuthorizationManagementService
        .getClientKey(_: UUID, _: String)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, persistentKey.kid, *, *)
        .once()
        .returns(Future.successful(persistentKey))

      Get() ~> service.getClientKeyById(persistentClient.id.toString, persistentKey.kid) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Key] should haveTheSameKey(expectedKey)
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]

      val kid = "some-kid"
      Get() ~> service.getClientKeyById(client.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-9989"
      }
    }

    "fail if client or key do not exist" in {
      val kid = "some-kid"
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(*, *, *)
        .once()
        .returns(Future.successful(persistentClient))

      (mockAuthorizationManagementService
        .getClientKey(_: UUID, _: String)(_: ExecutionContext, _: ReadModelService))
        .expects(*, *, *, *)
        .once()
        .returns(Future.failed(ClientKeyNotFound(client.id, kid)))

      Get() ~> service.getClientKeyById(client.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.NotFound
        responseAs[Problem].errors.head.code shouldEqual "007-0013"
      }
    }
  }

  "Retrieve all client keys" should {
    "succeed" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(*, *, *)
        .once()
        .returns(Future.successful(persistentClient.copy(users = Set(userId))))

      (mockAuthorizationManagementService
        .getClientKeys(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(Seq(persistentKey.copy(userId = Some(userId)))))

      val userIds = userId.toString

      Get() ~> service.getClientKeys(userIds, persistentClient.id.toString) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Keys] should haveTheSameKeys(Keys(Seq(expectedKey)))
      }
    }
    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      val userIds                                  = UUID.randomUUID.toString

      Get() ~> service.getClientKeys(userIds, UUID.randomUUID.toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-9989"
      }
    }

    "fail if client or key do not exist" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(*, *, *)
        .once()
        .returns(Future.successful(persistentClient))

      (mockAuthorizationManagementService
        .getClientKeys(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(*, *, *)
        .once()
        .returns(Future.failed(ClientNotFound(persistentClient.id)))

      val userIds = UUID.randomUUID.toString

      Get() ~> service.getClientKeys(userIds, persistentClient.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
        responseAs[Problem].errors.head.code shouldEqual "007-0010"
      }
    }
  }

  "Create client keys" should {
    "succeed" in {

      implicit val contexts: Seq[(String, String)] =
        Seq(
          "bearer"         -> bearerToken,
          USER_ROLES       -> ADMIN_ROLE,
          "organizationId" -> consumerId.toString,
          "uid"            -> userId.toString,
          "selfcareId"     -> selfcareId.toString
        )

      val keySeeds: Seq[KeySeed] = Seq(KeySeed(key = "key", use = KeyUse.SIG, alg = "123", name = "test"))

      (() => service.dateTimeSupplier.get()).expects().returning(timestamp).once()

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient.copy(users = Set(userId))))

      (mockAuthorizationManagementService
        .getClientKeys(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(Seq(persistentKey.copy(userId = Some(userId)))))

      val results: Seq[UserResource] = Seq(userResource)

      (mockSelfcareV2ClientService
        .getInstitutionProductUsers(_: UUID, _: UUID, _: Option[UUID], _: Seq[String])(
          _: Seq[(String, String)],
          _: ExecutionContext
        ))
        .expects(selfcareId, consumerId, userId.some, Seq(SECURITY_ROLE, ADMIN_ROLE), *, *)
        .once()
        .returns(Future.successful(results))

      (mockAuthorizationManagementService
        .createKeys(_: UUID, _: Seq[AuthorizationManagementDependency.KeySeed])(_: Seq[(String, String)]))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(AuthorizationManagementDependency.Keys(Seq(createdKey))))

      Get() ~> service.createKeys(persistentClient.id.toString, keySeeds) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Keys] should haveTheSameKeys(Keys(Seq(expectedKey)))
      }
    }

    "fail if missing authorization header" in {

      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]

      Get() ~> service.createKeys(client.id.toString, Seq.empty) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-9989"
      }
    }

    "fail if the uid in the header is not of the right user/admin of that consumer" in {

      implicit val contexts: Seq[(String, String)] =
        Seq(
          "bearer"         -> bearerToken,
          USER_ROLES       -> ADMIN_ROLE,
          "organizationId" -> consumerId.toString,
          "uid"            -> personId.toString,
          "selfcareId"     -> selfcareId.toString
        )

      val keySeeds: Seq[KeySeed] = Seq(KeySeed(key = "key", use = KeyUse.SIG, alg = "123", name = "test"))

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(client.id, *, *)
        .once()
        .returns(Future.successful(persistentClient))

      (mockAuthorizationManagementService
        .getClientKeys(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(client.id, *, *)
        .once()
        .returns(Future.successful(Seq(persistentKey.copy(userId = Some(userId)))))

      Get() ~> service.createKeys(client.id.toString, keySeeds) ~> check {
        status shouldEqual StatusCodes.Forbidden
        entityAs[Problem].errors.head.code shouldBe "007-0023"

      }
    }

    "fail if the keys exceed the maximum allowed" in {
      val keySeeds: Seq[KeySeed] = Seq(
        KeySeed(key = "key1", use = KeyUse.SIG, alg = "123", name = "test"),
        KeySeed(key = "key2", use = KeyUse.SIG, alg = "123", name = "test"),
        KeySeed(key = "key3", use = KeyUse.SIG, alg = "123", name = "test"),
        KeySeed(key = "key4", use = KeyUse.SIG, alg = "123", name = "test"),
        KeySeed(key = "key5", use = KeyUse.SIG, alg = "123", name = "test")
      )

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(client.id, *, *)
        .once()
        .returns(Future.successful(persistentClient))

      (mockAuthorizationManagementService
        .getClientKeys(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(client.id, *, *)
        .once()
        .returns(Future.successful(Seq(persistentKey.copy(userId = Some(userId)))))

      Get() ~> service.createKeys(client.id.toString, keySeeds) ~> check {
        status shouldEqual StatusCodes.BadRequest
        entityAs[Problem].errors.head.code shouldBe "007-0024"
      }
    }

    "fail if client or key do not exist" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(client.id, *, *)
        .once()
        .returns(Future.failed(ClientNotFound(client.id)))

      Get() ~> service.createKeys(client.id.toString, Seq.empty) ~> check {
        status shouldEqual StatusCodes.NotFound
        responseAs[Problem].errors.head.code shouldEqual "007-0010"
      }
    }
  }

  "Delete key" should {
    "succeed" in {
      val kid = "some-kid"

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(*, *, *)
        .once()
        .returns(Future.successful(persistentClient))

      (mockAuthorizationManagementService
        .deleteKey(_: UUID, _: String)(_: Seq[(String, String)]))
        .expects(client.id, kid, *)
        .once()
        .returns(Future.successful(()))

      Get() ~> service.deleteClientKeyById(client.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if client or key do not exist" in {
      val kid = "some-kid"
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(*, *, *)
        .once()
        .returns(Future.successful(persistentClient))

      (mockAuthorizationManagementService
        .deleteKey(_: UUID, _: String)(_: Seq[(String, String)]))
        .expects(*, *, *)
        .once()
        .returns(Future.failed(ClientKeyNotFound(client.id, kid)))

      Get() ~> service.deleteClientKeyById(client.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.NotFound
        responseAs[Problem].errors.head.code shouldEqual "007-0013"
      }
    }
  }
}
