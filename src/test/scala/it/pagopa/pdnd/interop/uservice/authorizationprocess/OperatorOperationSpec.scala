package it.pagopa.pdnd.interop.uservice.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.ClientApiServiceImpl
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.PartyManagementService
import it.pagopa.pdnd.interop.uservice.authorizationprocess.util.SpecUtils
import it.pagopa.pdnd.interop.uservice.keymanagement
import it.pagopa.pdnd.interop.uservice.partymanagement
import it.pagopa.pdnd.interop.uservice.partymanagement.client.model.{
  PersonSeed,
  Relationship,
  RelationshipEnums,
  RelationshipSeed,
  RelationshipSeedEnums,
  Relationships
}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.{ExecutionContext, Future}

class OperatorOperationSpec extends AnyWordSpecLike with MockFactory with SpecUtils with ScalatestRouteTest {
  import clientApiMarshaller._

  val service: ClientApiServiceImpl = ClientApiServiceImpl(
    mockAuthorizationManagementService,
    mockAgreementManagementService,
    mockCatalogManagementService,
    mockPartyManagementService
  )(ExecutionContext.global)

  "Operator addition" should {
    "succeed on existing relationship" in {
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
        purposes = client.purposes,
        description = client.description,
        status = client.status.toString,
        operators = Some(Seq(operator))
      )

      Get() ~> service.addOperator(client.id.toString, operatorSeed) ~> check {
        status shouldEqual StatusCodes.Created
        entityAs[Client] shouldEqual expected
      }
    }

    "succeed creating relationship" in {
      (mockAuthorizationManagementService.getClient _)
        .expects(client.id.toString)
        .once()
        .returns(Future.successful(client))

      (mockPartyManagementService.getOrganization _)
        .expects(client.consumerId)
        .once()
        .returns(Future.successful(organization))

      // Missing relationship
      (mockPartyManagementService.getRelationships _)
        .expects(organization.institutionId, operatorSeed.taxCode, PartyManagementService.ROLE_SECURITY_OPERATOR)
        .once()
        .returns(Future.failed(partymanagement.client.invoker.ApiError(404, "Some message", None)))

      // Existing person
      (mockPartyManagementService.getPersonByTaxCode _)
        .expects(operatorSeed.taxCode)
        .once()
        .returns(Future.successful(person))

      (mockPartyManagementService.getOrganization _)
        .expects(client.consumerId)
        .once()
        .returns(Future.successful(organization))

      (mockPartyManagementService.createRelationship _)
        .expects(
          RelationshipSeed(
            operatorSeed.taxCode,
            organization.institutionId,
            RelationshipSeedEnums.Role.Operator,
            PartyManagementService.ROLE_SECURITY_OPERATOR
          )
        )
        .once()
        .returns(Future.successful(relationship))

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
        purposes = client.purposes,
        description = client.description,
        status = client.status.toString,
        operators = Some(Seq(operator))
      )

      Get() ~> service.addOperator(client.id.toString, operatorSeed) ~> check {
        status shouldEqual StatusCodes.Created
        entityAs[Client] shouldEqual expected
      }
    }

    "succeed creating relationship and person" in {
      (mockAuthorizationManagementService.getClient _)
        .expects(client.id.toString)
        .once()
        .returns(Future.successful(client))

      (mockPartyManagementService.getOrganization _)
        .expects(client.consumerId)
        .once()
        .returns(Future.successful(organization))

      // Missing relationship
      (mockPartyManagementService.getRelationships _)
        .expects(organization.institutionId, operatorSeed.taxCode, PartyManagementService.ROLE_SECURITY_OPERATOR)
        .once()
        .returns(Future.failed(partymanagement.client.invoker.ApiError(404, "Some message", None)))

      // Missing person
      (mockPartyManagementService.getPersonByTaxCode _)
        .expects(operatorSeed.taxCode)
        .once()
        .returns(Future.failed(partymanagement.client.invoker.ApiError(404, "Some message", None)))

      (mockPartyManagementService.createPerson _)
        .expects(PersonSeed(operatorSeed.taxCode, operatorSeed.surname, operatorSeed.name))
        .once()
        .returns(Future.successful(person))

      (mockPartyManagementService.getOrganization _)
        .expects(client.consumerId)
        .once()
        .returns(Future.successful(organization))

      (mockPartyManagementService.createRelationship _)
        .expects(
          RelationshipSeed(
            operatorSeed.taxCode,
            organization.institutionId,
            RelationshipSeedEnums.Role.Operator,
            PartyManagementService.ROLE_SECURITY_OPERATOR
          )
        )
        .once()
        .returns(Future.successful(relationship))

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
        purposes = client.purposes,
        description = client.description,
        status = client.status.toString,
        operators = Some(Seq(operator))
      )

      Get() ~> service.addOperator(client.id.toString, operatorSeed) ~> check {
        status shouldEqual StatusCodes.Created
        entityAs[Client] shouldEqual expected
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      val seed                                     = operatorSeed

      Get() ~> service.addOperator(client.id.toString, seed) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if client does not exist" in {
      (mockAuthorizationManagementService.getClient _)
        .expects(*)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "Some message", None)))

      Get() ~> service.addOperator(client.id.toString, operatorSeed) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "fail if operator is already assigned" in {
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

      Get() ~> service.addOperator(client.id.toString, operatorSeed) ~> check {
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

  "Operator retrieve by external id" should {
    "succeed" in {
      (mockAuthorizationManagementService.getClient _)
        .expects(client.id.toString)
        .once()
        .returns(Future.successful(client.copy(relationships = Set(relationship.id))))

      (mockPartyManagementService.getRelationshipById _)
        .expects(relationship.id)
        .once()
        .returns(Future.successful(relationship))

      (mockPartyManagementService.getPersonByTaxCode _)
        .expects(relationship.from)
        .once()
        .returns(Future.successful(person))

      val expected =
        Operator(
          taxCode = person.taxCode,
          name = person.name,
          surname = person.surname,
          role = relationship.role.toString,
          platformRole = relationship.platformRole
        )

      Get() ~> service.getClientOperatorByExternalId(client.id.toString, relationship.from) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Operator] shouldEqual expected
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]

      Get() ~> service.getClientOperatorByExternalId(client.id.toString, relationship.from) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if client does not exist" in {
      (mockAuthorizationManagementService.getClient _)
        .expects(client.id.toString)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "Some message", None)))

      Get() ~> service.getClientOperatorByExternalId(client.id.toString, relationship.from) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "fail if operator is not found" in {
      (mockAuthorizationManagementService.getClient _)
        .expects(client.id.toString)
        .once()
        .returns(Future.successful(client))

      Get() ~> service.getClientOperatorByExternalId(client.id.toString, relationship.from) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }
}
