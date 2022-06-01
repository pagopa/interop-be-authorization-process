package it.pagopa.interop.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.authorizationmanagement
import it.pagopa.interop.authorizationmanagement.client.api.{ClientApi, KeyApi, PurposeApi}
import it.pagopa.interop.authorizationmanagement.client.model.{KeysResponse, KeySeed => KeyMgmtSeed}
import it.pagopa.interop.authorizationprocess.api.impl.ClientApiServiceImpl
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationprocess.service.impl.AuthorizationManagementServiceImpl
import it.pagopa.interop.authorizationprocess.service.{AuthorizationManagementInvoker, PartyManagementService}
import it.pagopa.interop.authorizationprocess.util.{CustomMatchers, SpecUtilsWithImplicit}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class KeyOperationSpec
    extends AnyWordSpecLike
    with MockFactory
    with SpecUtilsWithImplicit
    with ScalatestRouteTest
    with CustomMatchers {
  import clientApiMarshaller._

  val service: ClientApiServiceImpl = ClientApiServiceImpl(
    mockAuthorizationManagementService,
    mockAgreementManagementService,
    mockCatalogManagementService,
    mockPartyManagementService,
    mockPurposeManagementService,
    mockUserRegistryManagementService
  )(ExecutionContext.global)

  val apiClientKey: ClientKey = ClientKey(
    name = "test",
    createdAt = OffsetDateTime.now(),
    key = Key(
      kty = createdKey.key.kty,
      key_ops = createdKey.key.keyOps,
      use = createdKey.key.use,
      alg = createdKey.key.alg,
      kid = createdKey.key.kid,
      x5u = createdKey.key.x5u,
      x5t = createdKey.key.x5t,
      x5tS256 = createdKey.key.x5tS256,
      x5c = createdKey.key.x5c,
      crv = createdKey.key.crv,
      x = createdKey.key.x,
      y = createdKey.key.y,
      d = createdKey.key.d,
      k = createdKey.key.k,
      n = createdKey.key.n,
      e = createdKey.key.e,
      p = createdKey.key.p,
      q = createdKey.key.q,
      dp = createdKey.key.dp,
      dq = createdKey.key.dq,
      qi = createdKey.key.qi,
      oth = createdKey.key.oth.map(_.map(info => OtherPrimeInfo(r = info.r, d = info.d, t = info.t)))
    )
  )

  "Retrieve key" should {
    "succeed" in {
      val kid = "some-kid"
      (mockAuthorizationManagementService
        .getKey(_: UUID, _: String)(_: Seq[(String, String)]))
        .expects(client.id, kid, *)
        .once()
        .returns(Future.successful(createdKey))

      (mockPartyManagementService
        .getRelationshipById(_: UUID)(_: Seq[(String, String)], _: ExecutionContext))
        .expects(*, *, *)
        .once()
        .returns(Future.successful(relationship))

      (mockUserRegistryManagementService
        .getUserById(_: UUID)(_: Seq[(String, String)]))
        .expects(*, *)
        .once()
        .returns(Future.successful(user))

      val expected = ReadClientKey(
        key = apiClientKey.key,
        name = apiClientKey.name,
        createdAt = apiClientKey.createdAt,
        operator = OperatorDetails(
          relationshipId = relationship.id,
          name = user.name.get.value,
          surname = user.familyName.get.value
        )
      )

      Get() ~> service.getClientKeyById(client.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[ReadClientKey] should haveTheSameReadKey(expected)
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      val service: ClientApiServiceImpl            = ClientApiServiceImpl(
        AuthorizationManagementServiceImpl(AuthorizationManagementInvoker(), ClientApi(), KeyApi(), PurposeApi()),
        mockAgreementManagementService,
        mockCatalogManagementService,
        mockPartyManagementService,
        mockPurposeManagementService,
        mockUserRegistryManagementService
      )(ExecutionContext.global)
      val kid                                      = "some-kid"
      Get() ~> service.getClientKeyById(client.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }

    "fail if client or key do not exist" in {
      val kid = "some-kid"
      (mockAuthorizationManagementService
        .getKey(_: UUID, _: String)(_: Seq[(String, String)]))
        .expects(*, *, *)
        .once()
        .returns(Future.failed(authorizationmanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.getClientKeyById(client.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Retrieve all client keys" should {
    "succeed" in {
      (mockAuthorizationManagementService
        .getClientKeys(_: UUID)(_: Seq[(String, String)]))
        .expects(client.id, *)
        .once()
        .returns(Future.successful(KeysResponse(Seq(createdKey))))

      (mockPartyManagementService
        .getRelationshipById(_: UUID)(_: Seq[(String, String)], _: ExecutionContext))
        .expects(*, *, *)
        .once()
        .returns(Future.successful(relationship))

      (mockUserRegistryManagementService
        .getUserById(_: UUID)(_: Seq[(String, String)]))
        .expects(*, *)
        .once()
        .returns(Future.successful(user))

      val expected = ReadClientKey(
        key = apiClientKey.key,
        name = apiClientKey.name,
        createdAt = apiClientKey.createdAt,
        operator = OperatorDetails(
          relationshipId = relationship.id,
          name = user.name.get.value,
          surname = user.familyName.get.value
        )
      )

      Get() ~> service.getClientKeys(client.id.toString) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[ReadClientKeys] should haveTheSameReadKeys(ReadClientKeys(Seq(expected)))
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      val service: ClientApiServiceImpl            = ClientApiServiceImpl(
        AuthorizationManagementServiceImpl(AuthorizationManagementInvoker(), ClientApi(), KeyApi(), PurposeApi()),
        mockAgreementManagementService,
        mockCatalogManagementService,
        mockPartyManagementService,
        mockPurposeManagementService,
        mockUserRegistryManagementService
      )(ExecutionContext.global)
      Get() ~> service.getClientKeys(client.id.toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }

    "fail if client or key do not exist" in {
      (mockAuthorizationManagementService
        .getClientKeys(_: UUID)(_: Seq[(String, String)]))
        .expects(*, *)
        .once()
        .returns(Future.failed(authorizationmanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.getClientKeys(client.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Create client keys" should {
    "succeed" in {
      val keySeeds: Seq[KeySeed] =
        Seq(KeySeed(operatorId = user.id, key = "key", use = KeyUse.SIG, alg = "123", name = "test"))

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(client.id, *)
        .once()
        .returns(Future.successful(client))

      (mockPartyManagementService
        .getRelationships(_: UUID, _: UUID, _: Seq[String])(_: Seq[(String, String)], _: ExecutionContext))
        .expects(client.consumerId, user.id, Seq(PartyManagementService.PRODUCT_ROLE_SECURITY_OPERATOR), *, *)
        .once()
        .returns(Future.successful(relationships))

      (mockAuthorizationManagementService
        .createKeys(_: UUID, _: Seq[KeyMgmtSeed])(_: Seq[(String, String)]))
        .expects(client.id, *, *)
        .once()
        .returns(Future.successful(KeysResponse(Seq(createdKey))))

      val expected = apiClientKey

      Get() ~> service.createKeys(client.id.toString, keySeeds) ~> check {
        status shouldEqual StatusCodes.Created
        entityAs[ClientKeys] should haveTheSameKeys(ClientKeys(Seq(expected)))
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      val service: ClientApiServiceImpl            = ClientApiServiceImpl(
        AuthorizationManagementServiceImpl(AuthorizationManagementInvoker(), ClientApi(), KeyApi(), PurposeApi()),
        mockAgreementManagementService,
        mockCatalogManagementService,
        mockPartyManagementService,
        mockPurposeManagementService,
        mockUserRegistryManagementService
      )(ExecutionContext.global)
      Get() ~> service.createKeys(client.id.toString, Seq.empty) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }

    "fail if client or key do not exist" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(client.id, *)
        .once()
        .returns(Future.failed(authorizationmanagement.client.invoker.ApiError(404, "Some message", None)))

      Get() ~> service.createKeys(client.id.toString, Seq.empty) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Delete key" should {
    "succeed" in {
      val kid = "some-kid"
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
        .deleteKey(_: UUID, _: String)(_: Seq[(String, String)]))
        .expects(*, *, *)
        .once()
        .returns(Future.failed(authorizationmanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.deleteClientKeyById(client.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

}
