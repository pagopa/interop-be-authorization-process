package it.pagopa.pdnd.interop.uservice.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.ClientApiServiceImpl
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.AgreementManagementService.agreementStateToApi
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.AuthorizationManagementService.clientStateToApi
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.CatalogManagementService.descriptorStateToApi
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.PartyManagementService
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.PartyManagementService.{
  relationshipProductToApi,
  relationshipRoleToApi,
  relationshipStateToApi
}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.util.SpecUtils
import it.pagopa.pdnd.interop.uservice.keymanagement
import it.pagopa.pdnd.interop.uservice.partymanagement.client.{model => PartyManagementDependency}
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
    mockUserRegistryManagementService,
    mockJwtReader
  )(ExecutionContext.global)

  "Operator addition" should {
    "succeed on existing relationship" in {

      (mockJwtReader
        .getClaims(_: String))
        .expects(bearerToken)
        .returning(mockSubject(UUID.randomUUID().toString))
        .once()

      (mockAuthorizationManagementService.getClient _)
        .expects(client.id)
        .once()
        .returns(Future.successful(client))

      val activeRelationship: PartyManagementDependency.Relationship =
        relationship.copy(product = relationship.product.copy(role = PartyManagementService.ROLE_SECURITY_OPERATOR))

      (mockPartyManagementService
        .getRelationshipById(_: UUID)(_: String))
        .expects(UUID.fromString(relationshipId), bearerToken)
        .once()
        .returns(Future.successful(activeRelationship))

      (mockAuthorizationManagementService.addRelationship _)
        .expects(client.id, relationship.id)
        .once()
        .returns(Future.successful(client.copy(relationships = Set(relationship.id))))

      mockClientComposition(withOperators = true, relationship = activeRelationship)

      val expected = Client(
        id = client.id,
        eservice = EService(
          eService.id,
          eService.name,
          Organization(organization.institutionId, organization.description),
          Some(Descriptor(activeDescriptor.id, descriptorStateToApi(activeDescriptor.state), activeDescriptor.version))
        ),
        consumer = Organization(consumer.institutionId, consumer.description),
        agreement = Agreement(
          agreement.id,
          agreementStateToApi(agreement.state),
          Descriptor(activeDescriptor.id, descriptorStateToApi(activeDescriptor.state), activeDescriptor.version)
        ),
        name = client.name,
        purposes = client.purposes,
        description = client.description,
        state = clientStateToApi(client.state),
        operators = Some(
          Seq(operator.copy(product = operator.product.copy(role = PartyManagementService.ROLE_SECURITY_OPERATOR)))
        )
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
      (mockJwtReader
        .getClaims(_: String))
        .expects(bearerToken)
        .returning(mockSubject(UUID.randomUUID().toString))
        .once()

      (mockAuthorizationManagementService.getClient _)
        .expects(*)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "Some message", None)))

      Get() ~> service.clientOperatorRelationshipBinding(client.id.toString, relationshipId) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "fail if operator is already assigned" in {
      val operatorRelationship: PartyManagementDependency.Relationship = relationship.copy(
        state = PartyManagementDependency.RelationshipState.ACTIVE,
        product = relationship.product.copy(id = "PDND", role = PartyManagementService.ROLE_SECURITY_OPERATOR)
      )

      (mockJwtReader
        .getClaims(_: String))
        .expects(bearerToken)
        .returning(mockSubject(UUID.randomUUID().toString))
        .once()

      (mockAuthorizationManagementService.getClient _)
        .expects(client.id)
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

      (mockJwtReader
        .getClaims(_: String))
        .expects(bearerToken)
        .returning(mockSubject(UUID.randomUUID().toString))
        .once()

      (mockAuthorizationManagementService.removeClientRelationship _)
        .expects(client.id, relationship.id)
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

    "fail if client does not exist" in {
      (mockJwtReader
        .getClaims(_: String))
        .expects(bearerToken)
        .returning(mockSubject(UUID.randomUUID().toString))
        .once()

      (mockAuthorizationManagementService.removeClientRelationship _)
        .expects(client.id, relationship.id)
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
        product = relationship.product.copy(id = "PDND", role = PartyManagementService.ROLE_SECURITY_OPERATOR)
      )

      (mockJwtReader
        .getClaims(_: String))
        .expects(bearerToken)
        .returning(mockSubject(UUID.randomUUID().toString))
        .once()

      (mockAuthorizationManagementService.getClient _)
        .expects(client.id)
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
          id = user.id,
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
      (mockJwtReader
        .getClaims(_: String))
        .expects(bearerToken)
        .returning(mockSubject(UUID.randomUUID().toString))
        .once()

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
      (mockJwtReader
        .getClaims(_: String))
        .expects(bearerToken)
        .returning(mockSubject(UUID.randomUUID().toString))
        .once()

      (mockAuthorizationManagementService.getClient _)
        .expects(client.id)
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
          id = user.id,
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
      (mockJwtReader
        .getClaims(_: String))
        .expects(bearerToken)
        .returning(mockSubject(UUID.randomUUID().toString))
        .once()

      (mockAuthorizationManagementService.getClient _)
        .expects(client.id)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "Some message", None)))

      Get() ~> service.getClientOperatorRelationshipById(client.id.toString, relationship.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "fail if operator is not found" in {
      (mockJwtReader
        .getClaims(_: String))
        .expects(bearerToken)
        .returning(mockSubject(UUID.randomUUID().toString))
        .once()

      (mockAuthorizationManagementService.getClient _)
        .expects(client.id)
        .once()
        .returns(Future.successful(client))

      Get() ~> service.getClientOperatorRelationshipById(client.id.toString, relationship.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }
}
