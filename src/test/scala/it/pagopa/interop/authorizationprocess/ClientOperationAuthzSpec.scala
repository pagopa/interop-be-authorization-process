package it.pagopa.interop.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.github.dwickern.macros.NameOf.nameOf
import it.pagopa.interop.authorizationprocess.api.impl.{ClientApiServiceImpl, problemFormat}
import it.pagopa.interop.authorizationprocess.util.{AuthorizedRoutes, SpecUtils}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.annotation.nowarn
import scala.concurrent.ExecutionContext

class ClientOperationAuthzSpec
    extends AnyWordSpecLike
    with MockFactory
    with SpecUtils
    with ScalatestRouteTest
    with Matchers {

  val service: ClientApiServiceImpl = ClientApiServiceImpl(
    mockAuthorizationManagementService,
    mockAgreementManagementService,
    mockCatalogManagementService,
    mockPartyManagementService,
    mockPurposeManagementService,
    mockUserRegistryManagementService
  )(ExecutionContext.global)

  "Client operation authorization spec" should {
    "authorized roles for delete Client" in {
      @nowarn
      val routeName = nameOf[ClientApiServiceImpl](_.deleteClient(???)(???, ???)) // getting method name
      val endpoint  = AuthorizedRoutes.endpoints(routeName)                       // getting endpoint authz details
      implicit val contexts = endpoint.contextsWithRandomRole // building a request context for an admittable role

      // when request occurs, check that it does not return neither 401 nor 403
      endpoint.asRequest ~> service.deleteClient(client.id.toString) ~> check {
        status should not be StatusCodes.Unauthorized
        status should not be StatusCodes.Forbidden
      }
    }
  }

}
