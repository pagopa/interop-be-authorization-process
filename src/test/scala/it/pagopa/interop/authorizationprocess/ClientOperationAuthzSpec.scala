package it.pagopa.interop.authorizationprocess

import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.github.dwickern.macros.NameOf.nameOf
import it.pagopa.interop.authorizationprocess.api.impl.{
  ClientApiServiceImpl,
  clientFormat,
  clientKeysFormat,
  clientsFormat,
  encodedClientKeyFormat,
  operatorFormat,
  problemFormat,
  readClientKeyFormat,
  readClientKeysFormat
}
import it.pagopa.interop.authorizationprocess.model.{ClientSeed, PurposeAdditionDetails}
import it.pagopa.interop.authorizationprocess.service._
import it.pagopa.interop.authorizationprocess.util.FakeDependencies._
import it.pagopa.interop.authorizationprocess.util.{AuthorizedRoutes, SpecUtils}
import it.pagopa.interop.commons.utils.USER_ROLES
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import spray.json.DefaultJsonProtocol.immSeqFormat

import java.util.UUID
import scala.annotation.nowarn
import scala.concurrent.ExecutionContext

class ClientOperationAuthzSpec
    extends AnyWordSpecLike
    with MockFactory
    with SpecUtils
    with ScalatestRouteTest
    with Matchers {

  val fakeAgreementManagementService: AgreementManagementService         = new FakeAgreementManagementService()
  val fakeCatalogManagementService: CatalogManagementService             = new FakeCatalogManagementService()
  val fakeAuthorizationManagementService: AuthorizationManagementService = new FakeAuthorizationManagementService()
  val fakePartyManagementService: PartyManagementService                 = new FakePartyManagementService()
  val fakePurposeManagementService: PurposeManagementService             = new FakePurposeManagementService()
  val fakeUserRegistryManagementService: UserRegistryManagementService   = new FakeUserRegistryManagementService()

  val service: ClientApiServiceImpl = ClientApiServiceImpl(
    fakeAuthorizationManagementService,
    fakeAgreementManagementService,
    fakeCatalogManagementService,
    fakePartyManagementService,
    fakePurposeManagementService,
    fakeUserRegistryManagementService
  )(ExecutionContext.global)

  // when request occurs, check that it does not return neither 401 nor 403
  def validRoleCheck(role: String, request: => HttpRequest, r: => Route) =
    request ~> r ~> check {
      status should not be StatusCodes.Unauthorized
      status should not be StatusCodes.Forbidden
      info(s"role $role is properly authorized")
    }

  // when request occurs, check that it forbids invalid role
  def invalidRoleCheck(role: String, request: => HttpRequest, r: => Route) = {
    request ~> r ~> check {
      status shouldBe StatusCodes.Forbidden
      info(s"role $role is properly forbidden since it is invalid")
    }
  }

  "Client operation authorization spec" should {
    "accept authorized roles for delete Client" in {
      @nowarn
      val routeName = nameOf[ClientApiServiceImpl](_.deleteClient(???)(???, ???))
      val endpoint  = AuthorizedRoutes.endpoints(routeName)

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(
          contexts.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.deleteClient(client.id.toString)
        )
      })

      implicit val invalidCtx = endpoint.contextsWithInvalidRole
      invalidRoleCheck(
        invalidCtx.toMap.get(USER_ROLES).toString,
        endpoint.asRequest,
        service.deleteClient(client.id.toString)
      )
    }

    "accept authorized roles for createConsumerClient" in {
      @nowarn
      val routeName = nameOf[ClientApiServiceImpl](_.createConsumerClient(???)(???, ???, ???))
      val endpoint  = AuthorizedRoutes.endpoints(routeName)

      val clientSeed = ClientSeed(consumerId = UUID.randomUUID(), name = "pippo")

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(
          contexts.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.createConsumerClient(clientSeed)
        )
      })

      implicit val invalidCtx = endpoint.contextsWithInvalidRole
      invalidRoleCheck(
        invalidCtx.toMap.get(USER_ROLES).toString,
        endpoint.asRequest,
        service.createConsumerClient(clientSeed)
      )
    }
    "accept authorized roles for createApiClient" in {
      @nowarn
      val routeName = nameOf[ClientApiServiceImpl](_.createApiClient(???)(???, ???, ???))
      val endpoint  = AuthorizedRoutes.endpoints(routeName)

      val clientSeed = ClientSeed(consumerId = UUID.randomUUID(), name = "pippo")

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(contexts.toMap.get(USER_ROLES).toString, endpoint.asRequest, service.createApiClient(clientSeed))
      })

      implicit val invalidCtx = endpoint.contextsWithInvalidRole
      invalidRoleCheck(
        invalidCtx.toMap.get(USER_ROLES).toString,
        endpoint.asRequest,
        service.createApiClient(clientSeed)
      )
    }

    "accept authorized roles for getClient" in {
      @nowarn
      val routeName = nameOf[ClientApiServiceImpl](_.getClient(???)(???, ???, ???))
      val endpoint  = AuthorizedRoutes.endpoints(routeName)

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(
          contexts.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.getClient(client.id.toString)
        )
      })

      implicit val invalidCtx = endpoint.contextsWithInvalidRole
      invalidRoleCheck(
        invalidCtx.toMap.get(USER_ROLES).toString,
        endpoint.asRequest,
        service.getClient(client.id.toString)
      )
    }
    "accept authorized roles for listClients" in {
      @nowarn
      val routeName =
        nameOf[ClientApiServiceImpl](_.listClients(???, ???, ???, ???, ???)(???, ???, ???))
      val endpoint  = AuthorizedRoutes.endpoints(routeName)

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(
          contexts.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.listClients(None, None, "test", None, None)
        )
      })

      implicit val invalidCtx = endpoint.contextsWithInvalidRole
      invalidRoleCheck(
        invalidCtx.toMap.get(USER_ROLES).toString,
        endpoint.asRequest,
        service.listClients(None, None, "test", None, None)
      )
    }
    "accept authorized roles for clientOperatorRelationshipBinding" in {
      @nowarn
      val routeName =
        nameOf[ClientApiServiceImpl](_.clientOperatorRelationshipBinding(???, ???)(???, ???, ???))
      val endpoint  = AuthorizedRoutes.endpoints(routeName)

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(
          contexts.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.clientOperatorRelationshipBinding("yada", "yada")
        )
      })

      implicit val invalidCtx = endpoint.contextsWithInvalidRole
      invalidRoleCheck(
        invalidCtx.toMap.get(USER_ROLES).toString,
        endpoint.asRequest,
        service.clientOperatorRelationshipBinding("inter", "op")
      )
    }
    "accept authorized roles for removeClientOperatorRelationship" in {
      @nowarn
      val routeName =
        nameOf[ClientApiServiceImpl](_.removeClientOperatorRelationship(???, ???)(???, ???))
      val endpoint  = AuthorizedRoutes.endpoints(routeName)

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(
          contexts.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.removeClientOperatorRelationship("yada", "yada")
        )
      })

      implicit val invalidCtx = endpoint.contextsWithInvalidRole
      invalidRoleCheck(
        invalidCtx.toMap.get(USER_ROLES).toString,
        endpoint.asRequest,
        service.removeClientOperatorRelationship("inter", "op")
      )
    }
    "accept authorized roles for getClientKeyById" in {
      @nowarn
      val routeName =
        nameOf[ClientApiServiceImpl](_.getClientKeyById(???, ???)(???, ???, ???))
      val endpoint  = AuthorizedRoutes.endpoints(routeName)

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(
          contexts.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.getClientKeyById("yada", "yada")
        )
      })

      implicit val invalidCtx = endpoint.contextsWithInvalidRole
      invalidRoleCheck(
        invalidCtx.toMap.get(USER_ROLES).toString,
        endpoint.asRequest,
        service.getClientKeyById("inter", "op")
      )
    }
    "accept authorized roles for deleteClientKeyById" in {
      @nowarn
      val routeName =
        nameOf[ClientApiServiceImpl](_.deleteClientKeyById(???, ???)(???, ???))
      val endpoint  = AuthorizedRoutes.endpoints(routeName)

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(
          contexts.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.deleteClientKeyById("yada", "yada")
        )
      })

      implicit val invalidCtx = endpoint.contextsWithInvalidRole
      invalidRoleCheck(
        invalidCtx.toMap.get(USER_ROLES).toString,
        endpoint.asRequest,
        service.deleteClientKeyById("inter", "op")
      )
    }
    "accept authorized roles for createKeys" in {
      @nowarn
      val routeName =
        nameOf[ClientApiServiceImpl](_.createKeys(???, ???)(???, ???, ???))
      val endpoint  = AuthorizedRoutes.endpoints(routeName)

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(
          contexts.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.createKeys("yada", Seq.empty)
        )
      })

      implicit val invalidCtx = endpoint.contextsWithInvalidRole
      invalidRoleCheck(
        invalidCtx.toMap.get(USER_ROLES).toString,
        endpoint.asRequest,
        service.createKeys("inter", Seq.empty)
      )
    }
    "accept authorized roles for getClientKeys" in {
      @nowarn
      val routeName =
        nameOf[ClientApiServiceImpl](_.getClientKeys(???)(???, ???, ???))
      val endpoint  = AuthorizedRoutes.endpoints(routeName)

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(contexts.toMap.get(USER_ROLES).toString, endpoint.asRequest, service.getClientKeys("yada"))
      })

      implicit val invalidCtx = endpoint.contextsWithInvalidRole
      invalidRoleCheck(invalidCtx.toMap.get(USER_ROLES).toString, endpoint.asRequest, service.getClientKeys("interop"))
    }
    "accept authorized roles for getClientOperators" in {
      @nowarn
      val routeName =
        nameOf[ClientApiServiceImpl](_.getClientOperators(???)(???, ???, ???))
      val endpoint  = AuthorizedRoutes.endpoints(routeName)

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(contexts.toMap.get(USER_ROLES).toString, endpoint.asRequest, service.getClientOperators("yada"))
      })

      implicit val invalidCtx = endpoint.contextsWithInvalidRole
      invalidRoleCheck(
        invalidCtx.toMap.get(USER_ROLES).toString,
        endpoint.asRequest,
        service.getClientOperators("interop")
      )
    }
    "accept authorized roles for getClientOperatorRelationshipById" in {
      @nowarn
      val routeName =
        nameOf[ClientApiServiceImpl](_.getClientOperatorRelationshipById(???, ???)(???, ???, ???))
      val endpoint  = AuthorizedRoutes.endpoints(routeName)

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(
          contexts.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.getClientOperatorRelationshipById("yada", "yada")
        )
      })

      implicit val invalidCtx = endpoint.contextsWithInvalidRole
      invalidRoleCheck(
        invalidCtx.toMap.get(USER_ROLES).toString,
        endpoint.asRequest,
        service.getClientOperatorRelationshipById("interop", "test")
      )
    }
    "accept authorized roles for addClientPurpose" in {
      @nowarn
      val routeName =
        nameOf[ClientApiServiceImpl](_.addClientPurpose(???, ???)(???, ???))
      val endpoint  = AuthorizedRoutes.endpoints(routeName)

      val details = PurposeAdditionDetails(UUID.randomUUID())

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(
          contexts.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.addClientPurpose("yada", details)
        )
      })

      implicit val invalidCtx = endpoint.contextsWithInvalidRole
      invalidRoleCheck(
        invalidCtx.toMap.get(USER_ROLES).toString,
        endpoint.asRequest,
        service.addClientPurpose("interop", details)
      )
    }
    "accept authorized roles for removeClientPurpose" in {
      @nowarn
      val routeName =
        nameOf[ClientApiServiceImpl](_.removeClientPurpose(???, ???)(???, ???))
      val endpoint  = AuthorizedRoutes.endpoints(routeName)

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(
          contexts.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.removeClientPurpose("yada", "test")
        )
      })

      implicit val invalidCtx = endpoint.contextsWithInvalidRole
      invalidRoleCheck(
        invalidCtx.toMap.get(USER_ROLES).toString,
        endpoint.asRequest,
        service.removeClientPurpose("interop", "test")
      )
    }
    "accept authorized roles for getEncodedClientKeyById" in {
      @nowarn
      val routeName =
        nameOf[ClientApiServiceImpl](_.getEncodedClientKeyById(???, ???)(???, ???, ???))
      val endpoint  = AuthorizedRoutes.endpoints(routeName)

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(
          contexts.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.getEncodedClientKeyById("yada", "test")
        )
      })

      implicit val invalidCtx = endpoint.contextsWithInvalidRole
      invalidRoleCheck(
        invalidCtx.toMap.get(USER_ROLES).toString,
        endpoint.asRequest,
        service.getEncodedClientKeyById("interop", "test")
      )
    }

  }
}
