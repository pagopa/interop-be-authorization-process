package it.pagopa.interop.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.authorizationmanagement.client.api.{ClientApi, KeyApi, PurposeApi}
import it.pagopa.interop.authorizationprocess.api.impl.{ClientApiServiceImpl, OperatorApiServiceImpl}
import it.pagopa.interop.authorizationprocess.api.impl.ClientApiMarshallerImpl._
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors.ClientNotFound
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationprocess.service.PartyManagementService.{
  relationshipProductToApi,
  relationshipRoleToApi,
  relationshipStateToApi
}
import it.pagopa.interop.authorizationprocess.service.impl.{
  AuthorizationManagementServiceImpl,
  PartyManagementServiceImpl
}
import it.pagopa.interop.authorizationprocess.service._
import it.pagopa.interop.selfcare.partymanagement.client.api.PartyApi
import it.pagopa.interop.selfcare.partymanagement.client.{model => PartyManagementDependency}
import it.pagopa.interop.authorizationmanagement.client.model.KeysResponse
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike
import it.pagopa.interop.authorizationprocess.util._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class OperatorOperationSpec
    extends AnyWordSpecLike
    with MockFactory
    with SpecUtilsWithImplicit
    with ScalatestRouteTest {

  val serviceOperator: OperatorApiServiceImpl =
    OperatorApiServiceImpl(mockAuthorizationManagementService, mockPartyManagementService)(ExecutionContext.global)

  val service: ClientApiServiceImpl = ClientApiServiceImpl(
    mockAuthorizationManagementService,
    mockAgreementManagementService,
    mockCatalogManagementService,
    mockPartyManagementService,
    mockPurposeManagementService,
    mockUserRegistryManagementService,
    mockTenantManagementService,
    mockReadModel,
    mockDateTimeSupplier
  )(ExecutionContext.global)

  "Operator addition" should {
    "succeed on existing relationship" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(client.id, *)
        .once()
        .returns(Future.successful(client))

      val activeRelationship: PartyManagementDependency.Relationship =
        relationship.copy(product =
          relationship.product.copy(role = PartyManagementService.PRODUCT_ROLE_SECURITY_OPERATOR)
        )

      (mockPartyManagementService
        .getRelationshipById(_: UUID)(_: Seq[(String, String)], _: ExecutionContext))
        .expects(UUID.fromString(relationshipId), *, *)
        .once()
        .returns(Future.successful(activeRelationship))

      (mockAuthorizationManagementService
        .addRelationship(_: UUID, _: UUID)(_: Seq[(String, String)]))
        .expects(client.id, relationship.id, *)
        .once()
        .returns(Future.successful(client.copy(relationships = Set(relationship.id))))

      val expected = Client(
        id = client.id,
        consumerId = consumerId,
        name = client.name,
        purposes = Seq(clientPurposeProcess),
        description = client.description,
        relationshipsIds = Set(relationship.id),
        kind = ClientKind.CONSUMER,
        createdAt = timestamp
      )

      Get() ~> service.clientOperatorRelationshipBinding(client.id.toString, relationshipId) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Client] shouldEqual expected
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      val seed                                     = relationshipId
      val service: ClientApiServiceImpl            = ClientApiServiceImpl(
        AuthorizationManagementServiceImpl(
          AuthorizationManagementInvoker(ExecutionContext.global),
          ClientApi(),
          KeyApi(),
          PurposeApi()
        ),
        mockAgreementManagementService,
        mockCatalogManagementService,
        mockPartyManagementService,
        mockPurposeManagementService,
        mockUserRegistryManagementService,
        mockTenantManagementService,
        mockReadModel,
        mockDateTimeSupplier
      )(ExecutionContext.global)
      Get() ~> service.clientOperatorRelationshipBinding(client.id.toString, seed) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }

    "fail if client does not exist" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(*, *)
        .once()
        .returns(Future.failed(ClientNotFound(client.id)))

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
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(client.id, *)
        .once()
        .returns(Future.successful(client.copy(relationships = Set(operatorRelationship.id))))

      (mockPartyManagementService
        .getRelationshipById(_: UUID)(_: Seq[(String, String)], _: ExecutionContext))
        .expects(UUID.fromString(relationshipId), *, *)
        .once()
        .returns(Future.successful(operatorRelationship))

      Get() ~> service.clientOperatorRelationshipBinding(client.id.toString, relationshipId) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "Operator removal" should {
    "succeed" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(*, *)
        .once()
        .returns(Future.successful(client))

      (mockPartyManagementService
        .getRelationshipsByPersonId(_: UUID, _: Seq[String])(_: Seq[(String, String)], _: ExecutionContext))
        .expects(personId, Seq.empty, *, *)
        .once()
        .returns(Future.successful(relationships.copy(items = Seq.empty)))

      (mockAuthorizationManagementService
        .removeClientRelationship(_: UUID, _: UUID)(_: Seq[(String, String)]))
        .expects(client.id, relationship.id, *)
        .once()
        .returns(Future.successful(()))

      Get() ~> service.removeClientOperatorRelationship(client.id.toString, relationship.id.toString) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      val service: ClientApiServiceImpl            = ClientApiServiceImpl(
        mockAuthorizationManagementService,
        mockAgreementManagementService,
        mockCatalogManagementService,
        PartyManagementServiceImpl(PartyManagementInvoker(), PartyApi())(PartyManagementApiKeyValue()),
        mockPurposeManagementService,
        mockUserRegistryManagementService,
        mockTenantManagementService,
        mockReadModel,
        mockDateTimeSupplier
      )(ExecutionContext.global)
      Get() ~> service.removeClientOperatorRelationship(client.id.toString, relationship.id.toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }

    "succeed if an admin user removes own relationship" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(*, *)
        .once()
        .returns(Future.successful(client))

      val relationshipId    = UUID.randomUUID()
      val userRelationships = relationships.copy(items =
        Seq(
          relationship.copy(
            id = relationshipId,
            product = relationship.product.copy(role = PartyManagementService.PRODUCT_ROLE_ADMIN)
          )
        )
      )

      (mockPartyManagementService
        .getRelationshipsByPersonId(_: UUID, _: Seq[String])(_: Seq[(String, String)], _: ExecutionContext))
        .expects(personId, Seq.empty, *, *)
        .once()
        .returns(Future.successful(userRelationships))

      (mockAuthorizationManagementService
        .removeClientRelationship(_: UUID, _: UUID)(_: Seq[(String, String)]))
        .expects(client.id, relationshipId, *)
        .once()
        .returns(Future.unit)

      Get() ~> service.removeClientOperatorRelationship(client.id.toString, relationshipId.toString) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if a security user removes own relationship" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(*, *)
        .once()
        .returns(Future.successful(client))

      val relationshipId    = UUID.randomUUID()
      val userRelationships = relationships.copy(items =
        Seq(
          relationship.copy(
            id = relationshipId,
            product = relationship.product.copy(role = PartyManagementService.PRODUCT_ROLE_SECURITY_OPERATOR)
          )
        )
      )

      (mockPartyManagementService
        .getRelationshipsByPersonId(_: UUID, _: Seq[String])(_: Seq[(String, String)], _: ExecutionContext))
        .expects(personId, Seq.empty, *, *)
        .once()
        .returns(Future.successful(userRelationships))

      Get() ~> service.removeClientOperatorRelationship(client.id.toString, relationshipId.toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[Problem].errors.head.code shouldEqual "007-0004"
      }
    }

    "fail if client does not exist" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(*, *)
        .once()
        .returns(Future.successful(client))

      (mockPartyManagementService
        .getRelationshipsByPersonId(_: UUID, _: Seq[String])(_: Seq[(String, String)], _: ExecutionContext))
        .expects(personId, Seq.empty, *, *)
        .once()
        .returns(Future.successful(relationships.copy(items = Seq.empty)))

      (mockAuthorizationManagementService
        .removeClientRelationship(_: UUID, _: UUID)(_: Seq[(String, String)]))
        .expects(client.id, relationship.id, *)
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
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(client.id, *)
        .once()
        .returns(Future.successful(client.copy(relationships = Set(operatorRelationship.id))))

      (mockPartyManagementService
        .getRelationshipById(_: UUID)(_: Seq[(String, String)], _: ExecutionContext))
        .expects(operatorRelationship.id, *, *)
        .once()
        .returns(Future.successful(operatorRelationship))

      (mockUserRegistryManagementService
        .getUserById(_: UUID)(_: Seq[(String, String)]))
        .expects(operatorRelationship.from, *)
        .once()
        .returns(Future.successful(user))

      val expected = Seq(
        Operator(
          relationshipId = UUID.fromString(relationshipId),
          taxCode = user.fiscalCode.get,
          name = user.name.get.value,
          familyName = user.familyName.get.value,
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
      val service: ClientApiServiceImpl            = ClientApiServiceImpl(
        AuthorizationManagementServiceImpl(
          AuthorizationManagementInvoker(ExecutionContext.global),
          ClientApi(),
          KeyApi(),
          PurposeApi()
        ),
        mockAgreementManagementService,
        mockCatalogManagementService,
        mockPartyManagementService,
        mockPurposeManagementService,
        mockUserRegistryManagementService,
        mockTenantManagementService,
        mockReadModel,
        mockDateTimeSupplier
      )(ExecutionContext.global)
      Get() ~> service.getClientOperators(client.id.toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }

    "fail if client does not exist" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(client.id, *)
        .once()
        .returns(Future.failed(ClientNotFound(client.id)))

      Get() ~> service.getClientOperators(client.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Operator retrieve keys" should {
    "succeed" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(client.id, *)
        .once()
        .returns(Future.successful(client.copy(consumerId = consumerId)))

      (mockPartyManagementService
        .getRelationshipsByPersonId(_: UUID, _: Seq[String])(_: Seq[(String, String)], _: ExecutionContext))
        .expects(consumerId, Seq.empty, *, *)
        .once()
        .returns(Future.successful(relationships.copy(items = Seq.empty)))

      (mockAuthorizationManagementService
        .getClientKeys(_: UUID)(_: Seq[(String, String)]))
        .expects(client.id, *)
        .once()
        .returns(Future.successful(KeysResponse(Seq(createdKey))))

      Get() ~> serviceOperator.getClientOperatorKeys(client.id.toString, client.consumerId.toString) ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "fail if the caller is not the client consumer" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(client.id, *)
        .once()
        .returns(Future.successful(client.copy(consumerId = UUID.randomUUID())))

      Get() ~> serviceOperator.getClientOperatorKeys(client.id.toString, UUID.randomUUID().toString) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }
  }
}
