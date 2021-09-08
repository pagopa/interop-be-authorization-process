package it.pagopa.pdnd.interop.uservice.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.AuthApiServiceImpl
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
