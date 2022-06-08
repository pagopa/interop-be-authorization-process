package it.pagopa.interop.authorizationprocess.authz

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
      val endpoint = AuthorizedRoutes.endpoints("deleteClient")

      // for each role of this route, it checks if it is properly authorized
      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(contexts.toMap.get(USER_ROLES).toString, endpoint.asRequest, service.deleteClient("fake"))
      })

      // given a fake role, check that its invocation is forbidden
      endpoint.invalidRoles.foreach(contexts => {
        implicit val invalidCtx = contexts
        invalidRoleCheck(invalidCtx.toMap.get(USER_ROLES).toString, endpoint.asRequest, service.deleteClient("fake"))
      })
    }

    "accept authorized roles for createConsumerClient" in {
      val endpoint = AuthorizedRoutes.endpoints("createConsumerClient")

      val clientSeed = ClientSeed(consumerId = UUID.randomUUID(), name = "pippo")

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(
          contexts.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.createConsumerClient(clientSeed)
        )
      })

      endpoint.invalidRoles.foreach(contexts => {
        implicit val invalidCtx = contexts
        invalidRoleCheck(
          invalidCtx.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.createConsumerClient(clientSeed)
        )
      })
    }
    "accept authorized roles for createApiClient" in {
      val endpoint = AuthorizedRoutes.endpoints("createApiClient")

      val clientSeed = ClientSeed(consumerId = UUID.randomUUID(), name = "pippo")

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(contexts.toMap.get(USER_ROLES).toString, endpoint.asRequest, service.createApiClient(clientSeed))
      })

      endpoint.invalidRoles.foreach(contexts => {
        implicit val invalidCtx = contexts
        invalidRoleCheck(
          invalidCtx.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.createApiClient(clientSeed)
        )
      })
    }

    "accept authorized roles for getClient" in {
      val endpoint = AuthorizedRoutes.endpoints("getClient")

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(contexts.toMap.get(USER_ROLES).toString, endpoint.asRequest, service.getClient("fake"))
      })

      endpoint.invalidRoles.foreach(contexts => {
        implicit val invalidCtx = contexts
        invalidRoleCheck(invalidCtx.toMap.get(USER_ROLES).toString, endpoint.asRequest, service.getClient("fake"))
      })
    }
    "accept authorized roles for listClients" in {
      val endpoint = AuthorizedRoutes.endpoints("listClients")

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(
          contexts.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.listClients(None, None, "test", None, None)
        )
      })

      endpoint.invalidRoles.foreach(contexts => {
        implicit val invalidCtx = contexts
        invalidRoleCheck(
          invalidCtx.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.listClients(None, None, "test", None, None)
        )
      })
    }
    "accept authorized roles for clientOperatorRelationshipBinding" in {
      val endpoint = AuthorizedRoutes.endpoints("clientOperatorRelationshipBinding")

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(
          contexts.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.clientOperatorRelationshipBinding("yada", "yada")
        )
      })

      endpoint.invalidRoles.foreach(contexts => {
        implicit val invalidCtx = contexts
        invalidRoleCheck(
          invalidCtx.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.clientOperatorRelationshipBinding("inter", "op")
        )
      })
    }
    "accept authorized roles for removeClientOperatorRelationship" in {
      val endpoint = AuthorizedRoutes.endpoints("removeClientOperatorRelationship")

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(
          contexts.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.removeClientOperatorRelationship("yada", "yada")
        )
      })

      endpoint.invalidRoles.foreach(contexts => {
        implicit val invalidCtx = contexts
        invalidRoleCheck(
          invalidCtx.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.removeClientOperatorRelationship("inter", "op")
        )
      })
    }
    "accept authorized roles for getClientKeyById" in {
      val endpoint = AuthorizedRoutes.endpoints("getClientKeyById")

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(
          contexts.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.getClientKeyById("yada", "yada")
        )
      })

      endpoint.invalidRoles.foreach(contexts => {
        implicit val invalidCtx = contexts
        invalidRoleCheck(
          invalidCtx.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.getClientKeyById("inter", "op")
        )
      })
    }
    "accept authorized roles for deleteClientKeyById" in {
      val endpoint = AuthorizedRoutes.endpoints("deleteClientKeyById")

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(
          contexts.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.deleteClientKeyById("yada", "yada")
        )
      })

      endpoint.invalidRoles.foreach(contexts => {
        implicit val invalidCtx = contexts
        invalidRoleCheck(
          invalidCtx.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.deleteClientKeyById("inter", "op")
        )
      })
    }
    "accept authorized roles for createKeys" in {
      val endpoint = AuthorizedRoutes.endpoints("createKeys")

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(
          contexts.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.createKeys("yada", Seq.empty)
        )
      })

      endpoint.invalidRoles.foreach(contexts => {
        implicit val invalidCtx = contexts
        invalidRoleCheck(
          invalidCtx.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.createKeys("inter", Seq.empty)
        )
      })
    }
    "accept authorized roles for getClientKeys" in {
      val endpoint = AuthorizedRoutes.endpoints("getClientKeys")

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(contexts.toMap.get(USER_ROLES).toString, endpoint.asRequest, service.getClientKeys("yada"))
      })

      endpoint.invalidRoles.foreach(contexts => {
        implicit val invalidCtx = contexts
        invalidRoleCheck(
          invalidCtx.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.getClientKeys("interop")
        )
      })
    }
    "accept authorized roles for getClientOperators" in {
      val endpoint = AuthorizedRoutes.endpoints("getClientOperators")

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(contexts.toMap.get(USER_ROLES).toString, endpoint.asRequest, service.getClientOperators("yada"))
      })

      endpoint.invalidRoles.foreach(contexts => {
        implicit val invalidCtx = contexts
        invalidRoleCheck(
          invalidCtx.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.getClientOperators("interop")
        )
      })
    }
    "accept authorized roles for getClientOperatorRelationshipById" in {
      val endpoint = AuthorizedRoutes.endpoints("getClientOperatorRelationshipById")

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(
          contexts.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.getClientOperatorRelationshipById("yada", "yada")
        )
      })

      endpoint.invalidRoles.foreach(contexts => {
        implicit val invalidCtx = contexts
        invalidRoleCheck(
          invalidCtx.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.getClientOperatorRelationshipById("interop", "test")
        )
      })
    }
    "accept authorized roles for addClientPurpose" in {
      val endpoint = AuthorizedRoutes.endpoints("addClientPurpose")

      val details = PurposeAdditionDetails(UUID.randomUUID())

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(
          contexts.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.addClientPurpose("yada", details)
        )
      })

      endpoint.invalidRoles.foreach(contexts => {
        implicit val invalidCtx = contexts
        invalidRoleCheck(
          invalidCtx.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.addClientPurpose("interop", details)
        )
      })
    }
    "accept authorized roles for removeClientPurpose" in {
      val endpoint = AuthorizedRoutes.endpoints("removeClientPurpose")

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(
          contexts.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.removeClientPurpose("yada", "test")
        )
      })

      endpoint.invalidRoles.foreach(contexts => {
        implicit val invalidCtx = contexts
        invalidRoleCheck(
          invalidCtx.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.removeClientPurpose("interop", "test")
        )
      })
    }
    "accept authorized roles for getEncodedClientKeyById" in {
      val endpoint = AuthorizedRoutes.endpoints("getEncodedClientKeyById")

      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(
          contexts.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.getEncodedClientKeyById("yada", "test")
        )
      })

      endpoint.invalidRoles.foreach(contexts => {
        implicit val invalidCtx = contexts
        invalidRoleCheck(
          invalidCtx.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.getEncodedClientKeyById("interop", "test")
        )
      })
    }

  }
}
