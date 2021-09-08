package it.pagopa.pdnd.interop.uservice.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.AuthApiServiceImpl
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.{Client, OperatorSeed}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.util.SpecUtils
import it.pagopa.pdnd.interop.uservice.keymanagement
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class OperatorOperationSpec extends AnyWordSpecLike with MockFactory with SpecUtils with ScalatestRouteTest {
  import authApiMarshaller._

  val service = new AuthApiServiceImpl(
    mockJwtValidator,
    mockJwtGenerator,
    mockAgreementProcessService,
    mockAgreementManagementService,
    mockAuthorizationManagementService
  )(ExecutionContext.global)

  "Operator addition" should {
    "succeed" in {
      val operatorId = UUID.randomUUID()
      (mockAuthorizationManagementService.addOperator _)
        .expects(createdClient.id, operatorId)
        .once()
        .returns(Future.successful(createdClient.copy(operators = Set(operatorId))))

      val expected = Client(
        id = createdClient.id,
        agreementId = createdClient.agreementId,
        description = createdClient.description,
        operators = Set(operatorId)
      )

      Get() ~> service.addOperator(createdClient.id.toString, OperatorSeed(operatorId)) ~> check {
        status shouldEqual StatusCodes.Created
        entityAs[Client] shouldEqual expected
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      val seed                                     = OperatorSeed(UUID.randomUUID())

      Get() ~> service.addOperator(createdClient.id.toString, seed) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if client does not exist" in {
      val seed = OperatorSeed(UUID.randomUUID())

      (mockAuthorizationManagementService.addOperator _)
        .expects(*, *)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "Some message", None)))

      Get() ~> service.addOperator(createdClient.id.toString, seed) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Operator removal" should {
    "succeed" in {
      val operatorId = UUID.randomUUID()
      (mockAuthorizationManagementService.removeClientOperator _)
        .expects(createdClient.id, operatorId)
        .once()
        .returns(Future.successful(()))

      Get() ~> service.removeClientOperator(createdClient.id.toString, operatorId.toString) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      val operatorId                               = UUID.randomUUID()

      Get() ~> service.removeClientOperator(createdClient.id.toString, operatorId.toString) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if client does not exist" in {
      val operatorId = UUID.randomUUID()

      (mockAuthorizationManagementService.removeClientOperator _)
        .expects(*, *)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "Some message", None)))

      Get() ~> service.removeClientOperator(createdClient.id.toString, operatorId.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }
}
