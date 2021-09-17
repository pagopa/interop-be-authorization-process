package it.pagopa.pdnd.interop.uservice.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.ClientApiServiceImpl
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.util.SpecUtils
import it.pagopa.pdnd.interop.uservice.keymanagement
import it.pagopa.pdnd.interop.uservice.keymanagement.client.model.KeysResponse
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class KeyOperationSpec extends AnyWordSpecLike with MockFactory with SpecUtils with ScalatestRouteTest {
  import clientApiMarshaller._

  val service = new ClientApiServiceImpl(
    mockAuthorizationManagementService,
    mockAgreementManagementService,
    mockCatalogManagementService,
    mockPartyManagementService
  )(ExecutionContext.global)

  val apiClientKey: ClientKey = ClientKey(
    status = createdKey.status.toString,
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
      (mockAuthorizationManagementService.getKey _)
        .expects(client.id, kid)
        .once()
        .returns(Future.successful(createdKey))

      val expected = apiClientKey

      Get() ~> service.getClientKeyById(client.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[ClientKey] shouldEqual expected
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      val kid                                      = "some-kid"
      Get() ~> service.getClientKeyById(client.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if client or key do not exist" in {
      val kid = "some-kid"
      (mockAuthorizationManagementService.getKey _)
        .expects(*, *)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.getClientKeyById(client.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Retrieve all client keys" should {
    "succeed" in {
      (mockAuthorizationManagementService.getClientKeys _)
        .expects(client.id)
        .once()
        .returns(Future.successful(KeysResponse(Seq(createdKey))))

      val expected = apiClientKey

      Get() ~> service.getClientKeys(client.id.toString) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[ClientKeys] shouldEqual ClientKeys(Seq(expected))
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      Get() ~> service.getClientKeys(client.id.toString) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if client or key do not exist" in {
      (mockAuthorizationManagementService.getClientKeys _)
        .expects(*)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.getClientKeys(client.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Create client keys" should {
    "succeed" in {
      val keySeeds: Seq[KeySeed] = Seq(
        KeySeed(
          operatorId = UUID.randomUUID(),
          key = "key",
          use = keymanagement.client.model.KeySeedEnums.Use.Sig.toString,
          alg = "123"
        )
      )

      (mockAuthorizationManagementService.createKeys _)
        .expects(client.id, *)
        .once()
        .returns(Future.successful(KeysResponse(Seq(createdKey))))

      val expected = apiClientKey

      Get() ~> service.createKeys(client.id.toString, keySeeds) ~> check {
        status shouldEqual StatusCodes.Created
        entityAs[ClientKeys] shouldEqual ClientKeys(Seq(expected))
      }
    }

    "fail on wrong enum parameters" in {
      val keySeeds: Seq[KeySeed] =
        Seq(KeySeed(operatorId = UUID.randomUUID(), key = "key", use = "non-existing-use-value", alg = "123"))

      Get() ~> service.createKeys(client.id.toString, keySeeds) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      Get() ~> service.createKeys(client.id.toString, Seq.empty) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if client or key do not exist" in {
      (mockAuthorizationManagementService.createKeys _)
        .expects(*, *)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.createKeys(client.id.toString, Seq.empty) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Delete key" should {
    "succeed" in {
      val kid = "some-kid"
      (mockAuthorizationManagementService.deleteKey _)
        .expects(client.id, kid)
        .once()
        .returns(Future.successful(()))

      Get() ~> service.deleteClientKeyById(client.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      val kid                                      = "some-kid"
      Get() ~> service.deleteClientKeyById(client.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if client or key do not exist" in {
      val kid = "some-kid"
      (mockAuthorizationManagementService.deleteKey _)
        .expects(*, *)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.deleteClientKeyById(client.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Enable key" should {
    "succeed" in {
      val kid = "some-kid"
      (mockAuthorizationManagementService.enableKey _)
        .expects(client.id, kid)
        .once()
        .returns(Future.successful(()))

      Get() ~> service.enableKeyById(client.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      val kid                                      = "some-kid"
      Get() ~> service.enableKeyById(client.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if client or key do not exist" in {
      val kid = "some-kid"
      (mockAuthorizationManagementService.enableKey _)
        .expects(*, *)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.enableKeyById(client.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Disable key" should {
    "succeed" in {
      val kid = "some-kid"
      (mockAuthorizationManagementService.disableKey _)
        .expects(client.id, kid)
        .once()
        .returns(Future.successful(()))

      Get() ~> service.disableKeyById(client.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      val kid                                      = "some-kid"
      Get() ~> service.disableKeyById(client.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if client or key do not exist" in {
      val kid = "some-kid"
      (mockAuthorizationManagementService.disableKey _)
        .expects(*, *)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.disableKeyById(client.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }
}
