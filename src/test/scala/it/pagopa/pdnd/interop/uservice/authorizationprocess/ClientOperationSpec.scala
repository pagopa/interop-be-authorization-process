package it.pagopa.pdnd.interop.uservice.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.AuthApiServiceImpl
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.{Client, ClientDetail, EService}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.util.SpecUtils
import it.pagopa.pdnd.interop.uservice.keymanagement
import it.pagopa.pdnd.interopuservice.catalogprocess
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.{ExecutionContext, Future}

class ClientOperationSpec extends AnyWordSpecLike with MockFactory with SpecUtils with ScalatestRouteTest {
  import authApiMarshaller._

  val service = new AuthApiServiceImpl(
    mockJwtValidator,
    mockJwtGenerator,
    mockAgreementProcessService,
    mockCatalogProcessService,
    mockAuthorizationManagementService
  )(ExecutionContext.global)

  "Client creation" should {
    "succeed" in {
      (mockCatalogProcessService.getEService _)
        .expects(bearerToken, clientSeed.eServiceId.toString)
        .once()
        .returns(Future.successful(eService))

      (mockAuthorizationManagementService.createClient _)
        .expects(clientSeed.eServiceId, clientSeed.name, clientSeed.description)
        .once()
        .returns(Future.successful(client))

      val expected = Client(
        id = client.id,
        eServiceId = client.eServiceId,
        name = client.name,
        description = client.description,
        operators = client.operators
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

    "fail if the E-Service does not exist" in {
      (mockCatalogProcessService.getEService _)
        .expects(bearerToken, clientSeed.eServiceId.toString)
        .once()
        .returns(Future.failed(catalogprocess.client.invoker.ApiError(404, "Some message", None)))

      Get() ~> service.createClient(clientSeed) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

  }

  "Client retrieve" should {
    "succeed" in {
      (mockAuthorizationManagementService.getClient _)
        .expects(*)
        .once()
        .returns(Future.successful(client))

      (mockCatalogProcessService.getEService _)
        .expects(*, client.eServiceId.toString)
        .once()
        .returns(Future.successful(eService))

      val expected =
        ClientDetail(
          id = client.id,
          eService = EService(eService.id, eService.name),
          name = client.name,
          description = client.description
        )

      Get() ~> service.getClient(client.id.toString) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[ClientDetail] shouldEqual expected
      }
    }

    "fail if client does not exist" in {
      (mockAuthorizationManagementService.getClient _)
        .expects(*)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.getClient(client.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Client list" should {
    "succeed" in {

      (mockAuthorizationManagementService.listClients _)
        .expects(*, *, *, *)
        .once()
        .returns(Future.successful(Seq(client)))

      (mockCatalogProcessService.getEService _)
        .expects(*, client.eServiceId.toString)
        .returns(Future.successful(eService))

      val expected = Seq(
        ClientDetail(
          id = client.id,
          eService = EService(eService.id, eService.name),
          name = client.name,
          description = client.description
        )
      )

      Get() ~> service.listClients(Some(0), Some(10), Some(eServiceId.toString), None) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Seq[ClientDetail]] shouldEqual expected
      }
    }
  }

  "Client delete" should {
    "succeed" in {
      (mockAuthorizationManagementService.deleteClient _)
        .expects(*)
        .once()
        .returns(Future.successful(()))

      Get() ~> service.deleteClient(client.id.toString) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if client does not exist" in {
      (mockAuthorizationManagementService.deleteClient _)
        .expects(*)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.deleteClient(client.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }
}
