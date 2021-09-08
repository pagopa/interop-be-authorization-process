package it.pagopa.pdnd.interop.uservice.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.AuthApiServiceImpl
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.Client
import it.pagopa.pdnd.interop.uservice.authorizationprocess.util.SpecUtils
import it.pagopa.pdnd.interop.uservice.agreementmanagement
import it.pagopa.pdnd.interop.uservice.keymanagement
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike
import it.pagopa.pdnd.interop.uservice.keymanagement

import scala.concurrent.{ExecutionContext, Future}

class ClientOperationSpec extends AnyWordSpecLike with MockFactory with SpecUtils with ScalatestRouteTest {
  import authApiMarshaller._

  val service = new AuthApiServiceImpl(
    mockJwtValidator,
    mockJwtGenerator,
    mockAgreementProcessService,
    mockAgreementManagementService,
    mockAuthorizationManagementService
  )(ExecutionContext.global)

  "Client creation" should {
    "succeed" in {
      (mockAgreementManagementService.retrieveAgreement _)
        .expects(bearerToken, clientSeed.agreementId.toString)
        .once()
        .returns(Future.successful(validAgreement))

      (mockAuthorizationManagementService.createClient _)
        .expects(clientSeed.agreementId, clientSeed.description)
        .once()
        .returns(Future.successful(createdClient))

      val expected = Client(
        id = createdClient.id,
        agreementId = createdClient.agreementId,
        description = createdClient.description,
        operators = createdClient.operators
      )

      Get() ~> service.createClient(clientSeed) ~> check {
        status shouldEqual StatusCodes.Created
        entityAs[Client] shouldEqual expected
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      Get() ~> service.createClient(clientSeed) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if the agreement does not exist" in {
      (mockAgreementManagementService.retrieveAgreement _)
        .expects(bearerToken, clientSeed.agreementId.toString)
        .once()
        .returns(Future.failed(agreementmanagement.client.invoker.ApiError(404, "Some message", None)))

      Get() ~> service.createClient(clientSeed) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "fail if the agreement is not in an expected status" in {
      (mockAgreementManagementService.retrieveAgreement _)
        .expects(bearerToken, clientSeed.agreementId.toString)
        .once()
        .returns(Future.successful(suspendedAgreement))

      Get() ~> service.createClient(clientSeed) ~> check {
        status shouldEqual StatusCodes.UnprocessableEntity
      }
    }

  }

  "Client retrieve" should {
    "succeed" in {
      (mockAuthorizationManagementService.getClient _)
        .expects(*)
        .once()
        .returns(Future.successful(createdClient))

      val expected =
        Client(
          id = createdClient.id,
          agreementId = createdClient.agreementId,
          description = createdClient.description,
          operators = createdClient.operators
        )

      Get() ~> service.getClient(createdClient.id.toString) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Client] shouldEqual expected
      }
    }

    "fail if client does not exist" in {
      (mockAuthorizationManagementService.getClient _)
        .expects(*)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.getClient(createdClient.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Client list" should {
    "succeed" in {

      (mockAuthorizationManagementService.listClients _)
        .expects(*, *, *, *)
        .once()
        .returns(Future.successful(Seq(createdClient)))

      val expected = Seq(
        Client(
          id = createdClient.id,
          agreementId = createdClient.agreementId,
          description = createdClient.description,
          operators = createdClient.operators
        )
      )

      Get() ~> service.listClients(Some(0), Some(10), Some(agreementId.toString), None) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Seq[Client]] shouldEqual expected
      }
    }
  }

  "Client delete" should {
    "succeed" in {
      (mockAuthorizationManagementService.deleteClient _)
        .expects(*)
        .once()
        .returns(Future.successful(()))

      Get() ~> service.deleteClient(createdClient.id.toString) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if client does not exist" in {
      (mockAuthorizationManagementService.deleteClient _)
        .expects(*)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.deleteClient(createdClient.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }
}