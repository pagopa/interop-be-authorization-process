package it.pagopa.interop.authorizationprocess.authz

import com.github.dwickern.macros.NameOf.nameOf
import it.pagopa.interop.authorizationprocess.api.impl.OperatorApiMarshallerImpl._
import it.pagopa.interop.authorizationprocess.api.impl.OperatorApiServiceImpl
import it.pagopa.interop.authorizationprocess.service._
import it.pagopa.interop.authorizationprocess.util.FakeDependencies._
import it.pagopa.interop.authorizationprocess.util.{AuthorizedRoutes, AuthzScalatestRouteTest}
import it.pagopa.interop.commons.utils.USER_ROLES
import org.scalamock.scalatest.MockFactory
import org.scalatest.wordspec.AnyWordSpecLike

import scala.annotation.nowarn
import scala.concurrent.ExecutionContext

class OperatorApiAuthzSpec extends AnyWordSpecLike with MockFactory with AuthzScalatestRouteTest {

  val fakeAgreementManagementService: AgreementManagementService         = new FakeAgreementManagementService()
  val fakeCatalogManagementService: CatalogManagementService             = new FakeCatalogManagementService()
  val fakeAuthorizationManagementService: AuthorizationManagementService = new FakeAuthorizationManagementService()
  val fakePartyManagementService: PartyManagementService                 = new FakePartyManagementService()
  val fakePurposeManagementService: PurposeManagementService             = new FakePurposeManagementService()
  val fakeUserRegistryManagementService: UserRegistryManagementService   = new FakeUserRegistryManagementService()

  val service: OperatorApiServiceImpl =
    OperatorApiServiceImpl(fakeAuthorizationManagementService, fakePartyManagementService)(ExecutionContext.global)

  "Operator api authorization spec" should {
    "accept authorized roles for getClientOperatorKeys" in {
      @nowarn
      val routeName = nameOf[OperatorApiServiceImpl](_.getClientOperatorKeys(???, ???)(???, ???, ???))
      val endpoint  = AuthorizedRoutes.endpoints(routeName)

      // for each role of this route, it checks if it is properly authorized
      endpoint.rolesInContexts.foreach(contexts => {
        implicit val ctx = contexts
        validRoleCheck(
          contexts.toMap.get(USER_ROLES).toString,
          endpoint.asRequest,
          service.getClientOperatorKeys("test", "test")
        )
      })

      // given a fake role, check that its invocation is forbidden
      implicit val invalidCtx = endpoint.contextsWithInvalidRole
      invalidRoleCheck(
        invalidCtx.toMap.get(USER_ROLES).toString,
        endpoint.asRequest,
        service.getClientOperatorKeys("test", "test")
      )
    }

  }
}
