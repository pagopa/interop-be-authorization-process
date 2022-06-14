package it.pagopa.interop.authorizationprocess.authz

import it.pagopa.interop.authorizationprocess.api.impl.OperatorApiMarshallerImpl._
import it.pagopa.interop.authorizationprocess.api.impl.OperatorApiServiceImpl
import it.pagopa.interop.authorizationprocess.service._
import it.pagopa.interop.authorizationprocess.util.FakeDependencies._
import it.pagopa.interop.authorizationprocess.util.{AuthorizedRoutes, AuthzScalatestRouteTest}
import org.scalamock.scalatest.MockFactory
import org.scalatest.wordspec.AnyWordSpecLike

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
      val endpoint = AuthorizedRoutes.endpoints("getClientOperatorKeys")
      validateAuthorization(
        endpoint,
        { implicit c: Seq[(String, String)] => service.getClientOperatorKeys("test", "test") }
      )
    }
  }
}
