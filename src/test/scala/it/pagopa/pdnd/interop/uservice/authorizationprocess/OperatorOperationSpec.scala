package it.pagopa.pdnd.interop.uservice.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.ClientApiServiceImpl
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.util.SpecUtils
import it.pagopa.pdnd.interop.uservice.keymanagement
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class OperatorOperationSpec extends AnyWordSpecLike with MockFactory with SpecUtils with ScalatestRouteTest {
  import clientApiMarshaller._

  val service = new ClientApiServiceImpl(
    mockAuthorizationManagementService,
    mockAgreementManagementService,
    mockCatalogManagementService,
    mockPartyManagementService
  )(ExecutionContext.global)

  "Operator addition" should {
    "succeed" in {
      val operatorId      = UUID.fromString(person.partyId)
      val operatorTaxCode = person.taxCode

      (mockAuthorizationManagementService.addOperator _)
        .expects(client.id, operatorId)
        .once()
        .returns(Future.successful(client.copy(operators = Set(operatorId))))

      (mockCatalogManagementService.getEService _)
        .expects(*, client.eServiceId.toString)
        .returns(Future.successful(eService))

      (mockPartyManagementService.getOrganization _)
        .expects(eService.producerId)
        .once()
        .returns(Future.successful(organization))

      (mockPartyManagementService.getOrganization _)
        .expects(client.consumerId)
        .once()
        .returns(Future.successful(consumer))

      (mockPartyManagementService.getPerson _)
        .expects(operatorId)
        .once()
        .returns(Future.successful(person))

      (mockPartyManagementService.getPersonByTaxCode _)
        .expects(operatorTaxCode)
        .once()
        .returns(Future.successful(person))

      (mockPartyManagementService.getRelationships _)
        .expects(organization.partyId, person.partyId)
        .once()
        .returns(Future.successful(relationships))

      (mockAgreementManagementService.getAgreements _)
        .expects(*, client.consumerId.toString, client.eServiceId.toString, None)
        .once()
        .returns(Future.successful(Seq(agreement)))

      val expected = Client(
        id = client.id,
        eservice = EService(
          eService.id,
          eService.name,
          Organization(organization.institutionId, organization.description),
          Some(Descriptor(activeDescriptor.id, activeDescriptor.status.toString, activeDescriptor.version))
        ),
        consumer = Organization(consumer.institutionId, consumer.description),
        agreement = Agreement(
          agreement.id,
          agreement.status.toString,
          Descriptor(activeDescriptor.id, activeDescriptor.status.toString, activeDescriptor.version)
        ),
        name = client.name,
        description = client.description,
        operators = Some(Seq(operator))
      )

      Get() ~> service.addOperator(client.id.toString, OperatorSeed(operatorTaxCode)) ~> check {
        status shouldEqual StatusCodes.Created
        entityAs[Client] shouldEqual expected
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      val seed                                     = OperatorSeed(person.taxCode)

      Get() ~> service.addOperator(client.id.toString, seed) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if client does not exist" in {
      val seed = OperatorSeed(person.taxCode)

      (mockPartyManagementService.getPersonByTaxCode _)
        .expects(person.taxCode)
        .once()
        .returns(Future.successful(person))

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
      val operatorId      = personId
      val operatorTaxCode = person.taxCode

      (mockAuthorizationManagementService.removeClientOperator _)
        .expects(client.id, operatorId)
        .once()
        .returns(Future.successful(()))

      (mockPartyManagementService.getPersonByTaxCode _)
        .expects(operatorTaxCode)
        .once()
        .returns(Future.successful(person))

      Get() ~> service.removeClientOperator(client.id.toString, operatorTaxCode) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      val operatorTaxCode                          = person.taxCode

      Get() ~> service.removeClientOperator(client.id.toString, operatorTaxCode) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if client does not exist" in {
      val operatorTaxCode = person.taxCode

      (mockPartyManagementService.getPersonByTaxCode _)
        .expects(person.taxCode)
        .once()
        .returns(Future.successful(person))

      (mockAuthorizationManagementService.removeClientOperator _)
        .expects(*, *)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "Some message", None)))

      Get() ~> service.removeClientOperator(client.id.toString, operatorTaxCode) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Operator retrieve" should {
    "succeed" in {
      (mockAuthorizationManagementService.getClient _)
        .expects(client.id.toString)
        .once()
        .returns(Future.successful(client.copy(operators = Set(personId))))

      (mockCatalogManagementService.getEService _)
        .expects(*, client.eServiceId.toString)
        .returns(Future.successful(eService))

      (mockPartyManagementService.getOrganization _)
        .expects(eService.producerId)
        .once()
        .returns(Future.successful(organization))

      (mockPartyManagementService.getPerson _)
        .expects(personId)
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
          role = relationships.items.head.role.toString,
          platformRole = relationships.items.head.platformRole
        )
      )

      Get() ~> service.getClientOperators(client.id.toString) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Seq[Operator]] shouldEqual expected
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]

      Get() ~> service.getClientOperators(client.id.toString) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if client does not exist" in {
      (mockAuthorizationManagementService.getClient _)
        .expects(client.id.toString)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "Some message", None)))

      Get() ~> service.getClientOperators(client.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }
}
