package it.pagopa.pdnd.interop.uservice.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.ClientApiServiceImpl
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.PartyManagementService
import it.pagopa.pdnd.interop.uservice.authorizationprocess.util.SpecUtils
import it.pagopa.pdnd.interop.uservice.keymanagement
import it.pagopa.pdnd.interop.uservice.partymanagement.client.model.{Relationship, RelationshipEnums, Relationships}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

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
      val operatorTaxCode = person.taxCode
//      val operatorRelationship: Relationship = relationship.copy(
//        status = RelationshipEnums.Status.Active,
//        platformRole = PartyManagementService.ROLE_SECURITY_OPERATOR
//      )

      (mockAuthorizationManagementService.getClient _)
        .expects(client.id.toString)
        .once()
        .returns(Future.successful(client))

      (mockPartyManagementService.getOrganization _)
        .expects(client.consumerId)
        .once()
        .returns(Future.successful(organization))

      (mockPartyManagementService.getRelationships _)
        .expects(organization.institutionId, person.taxCode, PartyManagementService.ROLE_SECURITY_OPERATOR)
        .once()
        .returns(Future.successful(Relationships(Seq(relationship))))

      (mockAuthorizationManagementService.addRelationship _)
        .expects(client.id, relationship.id)
        .once()
        .returns(Future.successful(client.copy(relationships = Set(relationship.id))))

      mockClientComposition(withOperators = true, relationship = relationship)

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

      (mockAuthorizationManagementService.getClient _)
        .expects(*)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "Some message", None)))

      Get() ~> service.addOperator(client.id.toString, seed) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "fail if operator is already assigned" in {
      val operatorTaxCode = person.taxCode
      val operatorRelationship: Relationship = relationship.copy(
        status = RelationshipEnums.Status.Active,
        platformRole = PartyManagementService.ROLE_SECURITY_OPERATOR
      )

      (mockAuthorizationManagementService.getClient _)
        .expects(client.id.toString)
        .once()
        .returns(Future.successful(client.copy(relationships = Set(operatorRelationship.id))))

      (mockPartyManagementService.getOrganization _)
        .expects(client.consumerId)
        .once()
        .returns(Future.successful(organization))

      (mockPartyManagementService.getRelationships _)
        .expects(organization.institutionId, person.taxCode, PartyManagementService.ROLE_SECURITY_OPERATOR)
        .once()
        .returns(Future.successful(Relationships(Seq(operatorRelationship))))

      Get() ~> service.addOperator(client.id.toString, OperatorSeed(operatorTaxCode)) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "Operator removal" should {
    "succeed" in {
      val operatorTaxCode = person.taxCode
      val operatorRelationship: Relationship = relationship.copy(
        status = RelationshipEnums.Status.Active,
        platformRole = PartyManagementService.ROLE_SECURITY_OPERATOR
      )

      (mockAuthorizationManagementService.getClient _)
        .expects(client.id.toString)
        .once()
        .returns(Future.successful(client.copy(relationships = Set(operatorRelationship.id))))

      (mockPartyManagementService.getOrganization _)
        .expects(client.consumerId)
        .once()
        .returns(Future.successful(organization))

      (mockPartyManagementService.getRelationships _)
        .expects(organization.institutionId, person.taxCode, PartyManagementService.ROLE_SECURITY_OPERATOR)
        .once()
        .returns(Future.successful(Relationships(Seq(operatorRelationship))))

      (mockAuthorizationManagementService.removeClientRelationship _)
        .expects(client.id, relationship.id)
        .once()
        .returns(Future.successful(()))

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

      (mockAuthorizationManagementService.getClient _)
        .expects(*)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "Some message", None)))

      Get() ~> service.removeClientOperator(client.id.toString, operatorTaxCode) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Operator retrieve" should {
    "succeed" in {
      val operatorRelationship: Relationship = relationship.copy(
        status = RelationshipEnums.Status.Active,
        platformRole = PartyManagementService.ROLE_SECURITY_OPERATOR
      )

      (mockAuthorizationManagementService.getClient _)
        .expects(client.id.toString)
        .once()
        .returns(Future.successful(client.copy(relationships = Set(operatorRelationship.id))))

      (mockPartyManagementService.getRelationshipById _)
        .expects(operatorRelationship.id)
        .once()
        .returns(Future.successful(operatorRelationship))

      (mockPartyManagementService.getPersonByTaxCode _)
        .expects(operatorRelationship.from)
        .once()
        .returns(Future.successful(person))

      val expected = Seq(
        Operator(
          taxCode = person.taxCode,
          name = person.name,
          surname = person.surname,
          role = operatorRelationship.role.toString,
          platformRole = operatorRelationship.platformRole
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
