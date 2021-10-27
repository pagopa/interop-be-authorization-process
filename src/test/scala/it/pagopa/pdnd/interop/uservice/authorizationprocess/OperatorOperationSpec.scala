package it.pagopa.pdnd.interop.uservice.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.ClientApiServiceImpl
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.PartyManagementService
import it.pagopa.pdnd.interop.uservice.authorizationprocess.util.SpecUtils
import it.pagopa.pdnd.interop.uservice.partymanagement.client.model.{
  Person => PartyPerson,
  PersonSeed => PartyPersonSeed,
  Relationship => PartyRelationship,
  RelationshipEnums => PartyRelationshipEnums,
  RelationshipSeed => PartyRelationshipSeed,
  RelationshipSeedEnums => PartyRelationshipSeedEnums,
  Relationships => PartyRelationships
}
import it.pagopa.pdnd.interop.uservice.userregistrymanagement.client.model.{NONE, UserExtras, UserSeed}
import it.pagopa.pdnd.interop.uservice.{keymanagement, partymanagement, userregistrymanagement}
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
    mockPartyManagementService,
    mockUserRegistryManagementService
  )(ExecutionContext.global)

  "Operator addition" should {
    "succeed on existing relationship" in {
      (mockAuthorizationManagementService.getClient _)
        .expects(client.id)
        .once()
        .returns(Future.successful(client))

      (mockUserRegistryManagementService.getUserByExternalId _)
        .expects(operatorSeed.taxCode)
        .once()
        .returns(Future.successful(user))

      (mockPartyManagementService.getRelationships _)
        .expects(client.consumerId, user.id, PartyManagementService.ROLE_SECURITY_OPERATOR)
        .once()
        .returns(Future.successful(PartyRelationships(Seq(relationship))))

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
        .expects(client.id)
        .once()
        .returns(Future.successful(client))

      // Existing person
      (mockUserRegistryManagementService.getUserByExternalId _)
        .expects(operatorSeed.taxCode)
        .once()
        .returns(Future.successful(user))

      // Missing relationship
      (mockPartyManagementService.getRelationships _)
        .expects(client.consumerId, user.id, PartyManagementService.ROLE_SECURITY_OPERATOR)
        .once()
        .returns(Future.failed(partymanagement.client.invoker.ApiError(404, "Some message", None)))

      (mockPartyManagementService.createRelationship _)
        .expects(
          PartyRelationshipSeed(
            user.id,
            client.consumerId,
            PartyRelationshipSeedEnums.Role.Operator,
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
        .expects(client.id)
        .once()
        .returns(Future.successful(client))

      // Missing person
      (mockUserRegistryManagementService.getUserByExternalId _)
        .expects(operatorSeed.taxCode)
        .once()
        .returns(Future.failed(userregistrymanagement.client.invoker.ApiError(404, "Some message", None)))

      (mockUserRegistryManagementService.createUser _)
        .expects(
          UserSeed(
            externalId = user.externalId,
            name = user.name,
            surname = user.surname,
            certification = NONE,
            extras = UserExtras(email = None, birthDate = None)
          )
        )
        .once()
        .returns(Future.successful(user))

      (mockPartyManagementService.createPerson _)
        .expects(PartyPersonSeed(user.id))
        .once()
        .returns(Future.successful(PartyPerson(user.id)))

      // Missing relationship
      (mockPartyManagementService.getRelationships _)
        .expects(client.consumerId, user.id, PartyManagementService.ROLE_SECURITY_OPERATOR)
        .once()
        .returns(Future.failed(partymanagement.client.invoker.ApiError(404, "Some message", None)))

      (mockPartyManagementService.createRelationship _)
        .expects(
          PartyRelationshipSeed(
            user.id,
            client.consumerId,
            PartyRelationshipSeedEnums.Role.Operator,
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
      val operatorRelationship: PartyRelationship = relationship.copy(
        status = PartyRelationshipEnums.Status.Active,
        platformRole = PartyManagementService.ROLE_SECURITY_OPERATOR
      )

      (mockAuthorizationManagementService.getClient _)
        .expects(client.id)
        .once()
        .returns(Future.successful(client.copy(relationships = Set(operatorRelationship.id))))

      (mockUserRegistryManagementService.getUserByExternalId _)
        .expects(operatorSeed.taxCode)
        .once()
        .returns(Future.successful(user))

      (mockPartyManagementService.getRelationships _)
        .expects(client.consumerId, user.id, PartyManagementService.ROLE_SECURITY_OPERATOR)
        .once()
        .returns(Future.successful(PartyRelationships(Seq(operatorRelationship))))

      Get() ~> service.addOperator(client.id.toString, operatorSeed) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "Operator removal" should {
    "succeed" in {
      val operatorId = user.id
      val operatorRelationship: PartyRelationship = relationship.copy(
        status = PartyRelationshipEnums.Status.Active,
        platformRole = PartyManagementService.ROLE_SECURITY_OPERATOR
      )

      (mockAuthorizationManagementService.getClient _)
        .expects(client.id)
        .once()
        .returns(Future.successful(client.copy(relationships = Set(operatorRelationship.id))))

      (mockPartyManagementService.getRelationships _)
        .expects(client.consumerId, operatorId, PartyManagementService.ROLE_SECURITY_OPERATOR)
        .once()
        .returns(Future.successful(PartyRelationships(Seq(operatorRelationship))))

      (mockAuthorizationManagementService.removeClientRelationship _)
        .expects(client.id, relationship.id)
        .once()
        .returns(Future.successful(()))

      Get() ~> service.removeClientOperator(client.id.toString, operatorId.toString) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]

      Get() ~> service.removeClientOperator(client.id.toString, user.id.toString) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if client does not exist" in {
      (mockAuthorizationManagementService.getClient _)
        .expects(*)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "Some message", None)))

      Get() ~> service.removeClientOperator(client.id.toString, user.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Operator retrieve" should {
    "succeed" in {
      val operatorRelationship: PartyRelationship = relationship.copy(
        status = PartyRelationshipEnums.Status.Active,
        platformRole = PartyManagementService.ROLE_SECURITY_OPERATOR
      )

      (mockAuthorizationManagementService.getClient _)
        .expects(client.id)
        .once()
        .returns(Future.successful(client.copy(relationships = Set(operatorRelationship.id))))

      (mockPartyManagementService.getRelationshipById _)
        .expects(operatorRelationship.id)
        .once()
        .returns(Future.successful(operatorRelationship))

      (mockUserRegistryManagementService.getUserById _)
        .expects(operatorRelationship.from)
        .once()
        .returns(Future.successful(user))

      val expected = Seq(
        Operator(
          id = user.id,
          taxCode = user.externalId,
          name = user.name,
          surname = user.surname,
          role = operatorRelationship.role.toString,
          platformRole = operatorRelationship.platformRole,
          // TODO Remove toLowerCase once defined standard for enums
          status = operatorRelationship.status.toString.toLowerCase
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
        .expects(client.id)
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
        .expects(client.id)
        .once()
        .returns(Future.successful(client.copy(relationships = Set(relationship.id))))

      (mockPartyManagementService.getRelationshipById _)
        .expects(relationship.id)
        .once()
        .returns(Future.successful(relationship))

      (mockUserRegistryManagementService.getUserById _)
        .expects(relationship.from)
        .once()
        .returns(Future.successful(user))

      val expected =
        Operator(
          id = user.id,
          taxCode = user.externalId,
          name = user.name,
          surname = user.surname,
          role = relationship.role.toString,
          platformRole = relationship.platformRole,
          // TODO Remove toLowerCase once defined standard for enums
          status = relationship.status.toString.toLowerCase
        )

      Get() ~> service.getClientOperatorById(client.id.toString, relationship.from.toString) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Operator] shouldEqual expected
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]

      Get() ~> service.getClientOperatorById(client.id.toString, relationship.from.toString) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if client does not exist" in {
      (mockAuthorizationManagementService.getClient _)
        .expects(client.id)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "Some message", None)))

      Get() ~> service.getClientOperatorById(client.id.toString, relationship.from.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "fail if operator is not found" in {
      (mockAuthorizationManagementService.getClient _)
        .expects(client.id)
        .once()
        .returns(Future.successful(client))

      Get() ~> service.getClientOperatorById(client.id.toString, relationship.from.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }
}
