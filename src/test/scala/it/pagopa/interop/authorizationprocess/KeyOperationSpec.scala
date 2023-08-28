package it.pagopa.interop.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.authorizationmanagement.client.api.{ClientApi, KeyApi, PurposeApi}
import it.pagopa.interop.authorizationmanagement.client.{model => AuthorizationManagementDependency}
import it.pagopa.interop.authorizationprocess.api.impl.ClientApiMarshallerImpl._
import it.pagopa.interop.authorizationprocess.api.impl.{ClientApiServiceImpl, keyFormat, keysFormat}
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors.{ClientKeyNotFound, ClientNotFound}
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationprocess.service.impl.AuthorizationManagementServiceImpl
import it.pagopa.interop.authorizationprocess.service.{AuthorizationManagementInvoker, PartyManagementService}
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
    mockPartyManagementService,
    mockPurposeManagementService,
    mockUserRegistryManagementService,
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
      val kid                                      = "some-kid"
      Get() ~> service.getClientKeyById(client.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.Forbidden
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
      }
    }
  }

  "Retrieve all client keys" should {
    "succeed" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(*, *, *)
        .once()
        .returns(Future.successful(persistentClient.copy(relationships = Set(relationship.id))))

      (mockAuthorizationManagementService
        .getClientKeys(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(Seq(persistentKey.copy(relationshipId = relationship.id))))

      val relationshipIds = relationship.id.toString

      Get() ~> service.getClientKeys(relationshipIds, persistentClient.id.toString) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Keys] should haveTheSameKeys(Keys(Seq(expectedKey)))
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      val relationshipIds                          = UUID.randomUUID.toString
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
      Get() ~> service.getClientKeys(relationshipIds, UUID.randomUUID.toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
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

      val relationshipIds = UUID.randomUUID.toString

      Get() ~> service.getClientKeys(relationshipIds, persistentClient.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Create client keys" should {
    "succeed" in {
      val keySeeds: Seq[KeySeed] = Seq(KeySeed(key = "key", use = KeyUse.SIG, alg = "123", name = "test"))

      (() => service.dateTimeSupplier.get()).expects().returning(timestamp).once()

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient))

      mockGetTenant()

      (mockPartyManagementService
        .getRelationships(_: String, _: UUID, _: Seq[String])(_: Seq[(String, String)], _: ExecutionContext))
        .expects(
          consumer.selfcareId.get,
          user.id,
          Seq(PartyManagementService.PRODUCT_ROLE_SECURITY_OPERATOR, PartyManagementService.PRODUCT_ROLE_ADMIN),
          *,
          *
        )
        .once()
        .returns(Future.successful(relationships))

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
      Get() ~> service.createKeys(client.id.toString, Seq.empty) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }

    "fail if the uid in the header is not of the right operator/admin of that consumer" in {
      val keySeeds: Seq[KeySeed] = Seq(KeySeed(key = "key", use = KeyUse.SIG, alg = "123", name = "test"))

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(client.id, *, *)
        .once()
        .returns(Future.successful(persistentClient))

      mockGetTenant()

      (mockPartyManagementService
        .getRelationships(_: String, _: UUID, _: Seq[String])(_: Seq[(String, String)], _: ExecutionContext))
        .expects(
          consumer.selfcareId.get,
          personId, // * This is the id present in the contexts
          Seq(PartyManagementService.PRODUCT_ROLE_SECURITY_OPERATOR, PartyManagementService.PRODUCT_ROLE_ADMIN),
          *,
          *
        )
        .once()
        .returns(Future.failed(AuthorizationProcessErrors.SecurityOperatorRelationshipNotFound(consumerId, personId)))

      Get() ~> service.createKeys(client.id.toString, keySeeds) ~> check {
        status shouldEqual StatusCodes.Forbidden
        entityAs[Problem].errors.head.code shouldBe "007-0003"
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
      }
    }
  }
}
