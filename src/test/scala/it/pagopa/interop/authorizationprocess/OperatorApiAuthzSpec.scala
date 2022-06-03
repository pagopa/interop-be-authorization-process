package it.pagopa.interop.authorizationprocess

import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.github.dwickern.macros.NameOf.nameOf
import it.pagopa.interop.authorizationprocess.api.impl.{OperatorApiServiceImpl, clientKeysFormat, problemFormat}
import it.pagopa.interop.authorizationprocess.service._
import it.pagopa.interop.authorizationprocess.util.FakeDependencies._
import it.pagopa.interop.authorizationprocess.util.{AuthorizedRoutes, SpecUtils}
import it.pagopa.interop.commons.utils.USER_ROLES
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.annotation.nowarn
import scala.concurrent.ExecutionContext

class OperatorApiAuthzSpec
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

  val service: OperatorApiServiceImpl =
    OperatorApiServiceImpl(fakeAuthorizationManagementService, fakePartyManagementService)(ExecutionContext.global)

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
