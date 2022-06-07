package it.pagopa.interop.authorizationprocess.authz

import com.github.dwickern.macros.NameOf.nameOf
import it.pagopa.interop.authorizationprocess.api.impl.ClientApiMarshallerImpl._
import it.pagopa.interop.authorizationprocess.api.impl.ClientApiServiceImpl
import it.pagopa.interop.authorizationprocess.model.{ClientSeed, PurposeAdditionDetails}
import it.pagopa.interop.authorizationprocess.service._
import it.pagopa.interop.authorizationprocess.util.FakeDependencies._
import it.pagopa.interop.authorizationprocess.util.{AuthorizedRoutes, AuthzScalatestRouteTest}
import it.pagopa.interop.commons.utils.USER_ROLES
import org.scalamock.scalatest.MockFactory
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.annotation.nowarn
import scala.concurrent.ExecutionContext

class ClientOperationAuthzSpec extends AnyWordSpecLike with MockFactory with AuthzScalatestRouteTest {

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

  "Client operation authorization spec" should {
    "accept authorized roles for delete Client" in {
      @nowarn
      val routeName = nameOf[ClientApiServiceImpl](_.deleteClient(???)(???, ???))
      val endpoint  = AuthorizedRoutes.endpoints(routeName)

      // for each role of this route, it checks if it is properly authorized
      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(contexts.toMap.get(USER_ROLES).toString, endpoint.asRequest, service.deleteClient("fake"))
      })

      // given a fake role, check that its invocation is forbidden
      implicit val invalidCtx = endpoint.contextsWithInvalidRole
      invalidRoleCheck(invalidCtx.toMap.get(USER_ROLES).toString, endpoint.asRequest, service.deleteClient("fake"))
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
        validRoleCheck(contexts.toMap.get(USER_ROLES).toString, endpoint.asRequest, service.getClient("fake"))
      })

      implicit val invalidCtx = endpoint.contextsWithInvalidRole
      invalidRoleCheck(invalidCtx.toMap.get(USER_ROLES).toString, endpoint.asRequest, service.getClient("fake"))
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
