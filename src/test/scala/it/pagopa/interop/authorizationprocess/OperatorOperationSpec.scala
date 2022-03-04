package it.pagopa.interop.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.authorizationmanagement
import it.pagopa.interop.authorizationprocess.api.impl.ClientApiServiceImpl
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationprocess.service.PartyManagementService.{
  relationshipProductToApi,
  relationshipRoleToApi,
  relationshipStateToApi
}
import it.pagopa.interop.authorizationprocess.service.{
  AgreementManagementService,
  AuthorizationManagementService,
  CatalogManagementService,
  PartyManagementService
}
import it.pagopa.interop.authorizationprocess.util.SpecUtils
import it.pagopa.interop.partymanagement.client.{model => PartyManagementDependency}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class OperatorOperationSpec extends AnyWordSpecLike with MockFactory with SpecUtils with ScalatestRouteTest {
  import clientApiMarshaller._

  val service: ClientApiServiceImpl = ClientApiServiceImpl(
    mockAuthorizationManagementService,
    mockAgreementManagementService,
    mockCatalogManagementService,
    mockPartyManagementService,
    mockPurposeManagementService,
    mockUserRegistryManagementService
  )(ExecutionContext.global)

  "Operator addition" should {
    "succeed on existing relationship" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: String))
        .expects(client.id, bearerToken)
        .once()
        .returns(Future.successful(client))

      val activeRelationship: PartyManagementDependency.Relationship =
        relationship.copy(product =
          relationship.product.copy(role = PartyManagementService.PRODUCT_ROLE_SECURITY_OPERATOR)
        )

      (mockPartyManagementService
        .getRelationshipById(_: UUID)(_: String))
        .expects(UUID.fromString(relationshipId), bearerToken)
        .once()
        .returns(Future.successful(activeRelationship))

      (mockAuthorizationManagementService
        .addRelationship(_: UUID, _: UUID)(_: String))
        .expects(client.id, relationship.id, bearerToken)
        .once()
        .returns(Future.successful(client.copy(relationships = Set(relationship.id))))

      mockClientComposition(withOperators = true, relationship = activeRelationship)

      val expectedAgreement: Agreement = Agreement(
        id = agreement.id,
        eservice = CatalogManagementService.eServiceToApi(eService),
        descriptor = CatalogManagementService.descriptorToApi(activeDescriptor.copy(id = agreement.descriptorId)),
        state = AgreementManagementService.stateToApi(agreement.state)
      )

      val expected = Client(
        id = client.id,
        consumer = Organization(consumer.institutionId, consumer.description),
        name = client.name,
        purposes = client.purposes.map(AuthorizationManagementService.purposeToApi(_, expectedAgreement)),
        description = client.description,
        operators = Some(
          Seq(
            operator.copy(product = operator.product.copy(role = PartyManagementService.PRODUCT_ROLE_SECURITY_OPERATOR))
          )
        ),
        kind = ClientKind.CONSUMER
      )

      Get() ~> service.clientOperatorRelationshipBinding(client.id.toString, relationshipId) ~> check {
        status shouldEqual StatusCodes.Created
        entityAs[Client] shouldEqual expected
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      val seed                                     = relationshipId

      Get() ~> service.clientOperatorRelationshipBinding(client.id.toString, seed) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if client does not exist" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: String))
        .expects(*, bearerToken)
        .once()
        .returns(Future.failed(authorizationmanagement.client.invoker.ApiError(404, "Some message", None)))

      Get() ~> service.clientOperatorRelationshipBinding(client.id.toString, relationshipId) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "fail if operator is already assigned" in {
      val operatorRelationship: PartyManagementDependency.Relationship = relationship.copy(
        state = PartyManagementDependency.RelationshipState.ACTIVE,
        product =
          relationship.product.copy(id = "Interop", role = PartyManagementService.PRODUCT_ROLE_SECURITY_OPERATOR)
      )

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: String))
        .expects(client.id, bearerToken)
        .once()
        .returns(Future.successful(client.copy(relationships = Set(operatorRelationship.id))))

      (mockPartyManagementService
        .getRelationshipById(_: UUID)(_: String))
        .expects(UUID.fromString(relationshipId), bearerToken)
        .once()
        .returns(Future.successful(operatorRelationship))

      Get() ~> service.clientOperatorRelationshipBinding(client.id.toString, relationshipId) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "Operator removal" should {
    "succeed" in {

      (mockPartyManagementService
        .getRelationshipsByPersonId(_: UUID, _: Seq[String])(_: String))
        .expects(personId, Seq.empty, bearerToken)
        .once()
        .returns(Future.successful(relationships.copy(items = Seq.empty)))

      (mockAuthorizationManagementService
        .removeClientRelationship(_: UUID, _: UUID)(_: String))
        .expects(client.id, relationship.id, bearerToken)
        .once()
        .returns(Future.successful(()))

      Get() ~> service.removeClientOperatorRelationship(client.id.toString, relationship.id.toString) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]

      Get() ~> service.removeClientOperatorRelationship(client.id.toString, relationship.id.toString) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if user removes own relationship" in {

      val relationshipId    = UUID.randomUUID()
      val userRelationships = relationships.copy(items = Seq(relationship.copy(id = relationshipId)))

      (mockPartyManagementService
        .getRelationshipsByPersonId(_: UUID, _: Seq[String])(_: String))
        .expects(personId, Seq.empty, bearerToken)
        .once()
        .returns(Future.successful(userRelationships))

      Get() ~> service.removeClientOperatorRelationship(client.id.toString, relationshipId.toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-0047"
      }
    }

    "fail if client does not exist" in {

      (mockPartyManagementService
        .getRelationshipsByPersonId(_: UUID, _: Seq[String])(_: String))
        .expects(personId, Seq.empty, bearerToken)
        .once()
        .returns(Future.successful(relationships.copy(items = Seq.empty)))

      (mockAuthorizationManagementService
        .removeClientRelationship(_: UUID, _: UUID)(_: String))
        .expects(client.id, relationship.id, bearerToken)
        .once()
        .returns(Future.failed(new RuntimeException("error")))

      Get() ~> service.removeClientOperatorRelationship(client.id.toString, relationship.id.toString) ~> check {
        status shouldEqual StatusCodes.InternalServerError
      }
    }
  }

  "Operator retrieve" should {
    "succeed" in {
      val operatorRelationship: PartyManagementDependency.Relationship = relationship.copy(
        state = PartyManagementDependency.RelationshipState.ACTIVE,
        product =
          relationship.product.copy(id = "Interop", role = PartyManagementService.PRODUCT_ROLE_SECURITY_OPERATOR)
      )

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: String))
        .expects(client.id, bearerToken)
        .once()
        .returns(Future.successful(client.copy(relationships = Set(operatorRelationship.id))))

      (mockPartyManagementService
        .getRelationshipById(_: UUID)(_: String))
        .expects(operatorRelationship.id, bearerToken)
        .once()
        .returns(Future.successful(operatorRelationship))

      (mockUserRegistryManagementService.getUserById _)
        .expects(operatorRelationship.from)
        .once()
        .returns(Future.successful(user))

      val expected = Seq(
        Operator(
          relationshipId = UUID.fromString(relationshipId),
          taxCode = user.externalId,
          name = user.name,
          surname = user.surname,
          role = relationshipRoleToApi(operatorRelationship.role),
          product = relationshipProductToApi(operatorRelationship.product),
          state = relationshipStateToApi(operatorRelationship.state)
            .getOrElse(throw new RuntimeException("Unexpected state during test"))
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
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: String))
        .expects(client.id, bearerToken)
        .once()
        .returns(Future.failed(authorizationmanagement.client.invoker.ApiError(404, "Some message", None)))

      Get() ~> service.getClientOperators(client.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Operator retrieve by external id" should {
    "succeed" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: String))
        .expects(client.id, bearerToken)
        .once()
        .returns(Future.successful(client.copy(relationships = Set(relationship.id))))

      (mockPartyManagementService
        .getRelationshipById(_: UUID)(_: String))
        .expects(relationship.id, bearerToken)
        .once()
        .returns(Future.successful(relationship))

      (mockUserRegistryManagementService.getUserById _)
        .expects(relationship.from)
        .once()
        .returns(Future.successful(user))

      val expected =
        Operator(
          relationshipId = UUID.fromString(relationshipId),
          taxCode = user.externalId,
          name = user.name,
          surname = user.surname,
          role = relationshipRoleToApi(relationship.role),
          product = relationshipProductToApi(relationship.product),
          state = relationshipStateToApi(relationship.state).getOrElse(
            throw new RuntimeException("Unexpected state during test")
          )
        )

      Get() ~> service.getClientOperatorRelationshipById(client.id.toString, relationship.id.toString) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Operator] shouldEqual expected
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]

      Get() ~> service.getClientOperatorRelationshipById(client.id.toString, relationship.id.toString) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if client does not exist" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: String))
        .expects(client.id, bearerToken)
        .once()
        .returns(Future.failed(authorizationmanagement.client.invoker.ApiError(404, "Some message", None)))

      Get() ~> service.getClientOperatorRelationshipById(client.id.toString, relationship.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "fail if operator is not found" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: String))
        .expects(client.id, bearerToken)
        .once()
        .returns(Future.successful(client))

      Get() ~> service.getClientOperatorRelationshipById(client.id.toString, relationship.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }
}
