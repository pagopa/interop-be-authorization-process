package it.pagopa.pdnd.interop.uservice.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.AuthApiServiceImpl
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.{Key, OtherPrimeInfo}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.util.SpecUtils
import it.pagopa.pdnd.interop.uservice.keymanagement
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.{ExecutionContext, Future}

class KeyOperationSpec extends AnyWordSpecLike with MockFactory with SpecUtils with ScalatestRouteTest {
  import authApiMarshaller._

  val service = new AuthApiServiceImpl(
    mockJwtValidator,
    mockJwtGenerator,
    mockAgreementProcessService,
    mockAgreementManagementService,
    mockAuthorizationManagementService
  )(ExecutionContext.global)

  "Retrieve key" should {
    "succeed" in {
      val kid = "some-kid"
      (mockAuthorizationManagementService.getKey _)
        .expects(createdClient.id, kid)
        .once()
        .returns(Future.successful(createdKey))

      val expected = Key(
        kty = createdKey.kty,
        key_ops = createdKey.keyOps,
        use = createdKey.use,
        alg = createdKey.alg,
        kid = createdKey.kid,
        x5u = createdKey.x5u,
        x5t = createdKey.x5t,
        x5tS256 = createdKey.x5tS256,
        x5c = createdKey.x5c,
        crv = createdKey.crv,
        x = createdKey.x,
        y = createdKey.y,
        d = createdKey.d,
        k = createdKey.k,
        n = createdKey.n,
        e = createdKey.e,
        p = createdKey.p,
        q = createdKey.q,
        dp = createdKey.dp,
        dq = createdKey.dq,
        qi = createdKey.qi,
        oth = createdKey.oth.map(_.map(info => OtherPrimeInfo(r = info.r, d = info.d, t = info.t)))
      )

      Get() ~> service.getClientKeyById(createdClient.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Key] shouldEqual expected
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      val kid                                      = "some-kid"
      Get() ~> service.getClientKeyById(createdClient.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if client or key do not exist" in {
      val kid = "some-kid"
      (mockAuthorizationManagementService.getKey _)
        .expects(*, *)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.getClientKeyById(createdClient.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Delete key" should {
    "succeed" in {
      val kid = "some-kid"
      (mockAuthorizationManagementService.deleteKey _)
        .expects(createdClient.id, kid)
        .once()
        .returns(Future.successful(()))

      Get() ~> service.deleteClientKeyById(createdClient.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      val kid                                      = "some-kid"
      Get() ~> service.deleteClientKeyById(createdClient.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if client or key do not exist" in {
      val kid = "some-kid"
      (mockAuthorizationManagementService.deleteKey _)
        .expects(*, *)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.deleteClientKeyById(createdClient.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Enable key" should {
    "succeed" in {
      val kid = "some-kid"
      (mockAuthorizationManagementService.enableKey _)
        .expects(createdClient.id, kid)
        .once()
        .returns(Future.successful(()))

      Get() ~> service.enableKeyById(createdClient.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      val kid                                      = "some-kid"
      Get() ~> service.enableKeyById(createdClient.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if client or key do not exist" in {
      val kid = "some-kid"
      (mockAuthorizationManagementService.enableKey _)
        .expects(*, *)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.enableKeyById(createdClient.id.toString, kid) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

}
