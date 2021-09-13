package it.pagopa.pdnd.interop.uservice.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.AuthApiServiceImpl
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.{Client, Operator, OperatorSeed}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.util.SpecUtils
import it.pagopa.pdnd.interop.uservice.keymanagement
import it.pagopa.pdnd.interop.uservice.partymanagement
import it.pagopa.pdnd.interop.uservice.partymanagement.client.model.RelationshipEnums
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
    mockAuthorizationManagementService,
    mockCatalogManagementService,
    mockPartyManagementService
  )(ExecutionContext.global)

  "Operator addition" should {
    "succeed" in {
      val operatorId = UUID.randomUUID()
      (mockAuthorizationManagementService.addOperator _)
        .expects(client.id, operatorId)
        .once()
        .returns(Future.successful(client.copy(operators = Set(operatorId))))

      val expected = Client(
        id = client.id,
        eServiceId = client.eServiceId,
        name = client.name,
        description = client.description,
        operators = Set(operatorId)
      )

      Get() ~> service.addOperator(client.id.toString, OperatorSeed(operatorId)) ~> check {
        status shouldEqual StatusCodes.Created
        entityAs[Client] shouldEqual expected
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      val seed                                     = OperatorSeed(UUID.randomUUID())

      Get() ~> service.addOperator(client.id.toString, seed) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if client does not exist" in {
      val seed = OperatorSeed(UUID.randomUUID())

      (mockAuthorizationManagementService.addOperator _)
        .expects(*, *)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "Some message", None)))

      Get() ~> service.addOperator(client.id.toString, seed) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Operator removal" should {
    "succeed" in {
      val operatorId = UUID.randomUUID()
      (mockAuthorizationManagementService.removeClientOperator _)
        .expects(client.id, operatorId)
        .once()
        .returns(Future.successful(()))

      Get() ~> service.removeClientOperator(client.id.toString, operatorId.toString) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      val operatorId                               = UUID.randomUUID()

      Get() ~> service.removeClientOperator(client.id.toString, operatorId.toString) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if client does not exist" in {
      val operatorId = UUID.randomUUID()

      (mockAuthorizationManagementService.removeClientOperator _)
        .expects(*, *)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "Some message", None)))

      Get() ~> service.removeClientOperator(client.id.toString, operatorId.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Operator retrieve" should {
    "succeed" in {
      val operator = UUID.randomUUID()

      val person = partymanagement.client.model.Person(
        taxCode = "taxCode",
        surname = "surname",
        name = "name",
        partyId = "partiyId"
      )

      val relationship =
        partymanagement.client.model.Relationship(
          from = organization.partyId,
          to = person.partyId,
          role = RelationshipEnums.Role.Operator,
          platformRole = "security",
          status = None
        )

      val relationships =
        partymanagement.client.model.Relationships(items = Seq(relationship))

      (mockAuthorizationManagementService.getClient _)
        .expects(client.id.toString)
        .once()
        .returns(Future.successful(client.copy(operators = Set(operator))))

      (mockCatalogManagementService.getEService _)
        .expects(*, client.eServiceId.toString)
        .returns(Future.successful(eService))

      (mockPartyManagementService.getOrganization _)
        .expects(eService.producerId)
        .once()
        .returns(Future.successful(organization))

      (mockPartyManagementService.getPerson _)
        .expects(operator)
        .once()
        .returns(Future.successful(person))

      (mockPartyManagementService.getRelationships _)
        .expects(organization.partyId, person.partyId)
        .once()
        .returns(Future.successful(relationships))

      val expected = Seq(
        Operator(
          taxCode = person.taxCode,
          name = person.name,
          surname = person.surname,
          email = "email",
          role = relationship.role.toString,
          platformRole = relationship.platformRole
        )
      )

      Get() ~> service.getClientOperators(client.id.toString) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Seq[Operator]] shouldEqual expected
      }
    }

  }
}
