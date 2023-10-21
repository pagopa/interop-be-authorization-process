package it.pagopa.interop.authorizationprocess.authz

import it.pagopa.interop.authorizationprocess.api.impl.UserApiMarshallerImpl._
import it.pagopa.interop.authorizationprocess.api.impl.UserApiServiceImpl
import it.pagopa.interop.authorizationprocess.service._
import it.pagopa.interop.authorizationprocess.util.FakeDependencies._
import it.pagopa.interop.authorizationprocess.util.{AuthorizedRoutes, AuthzScalatestRouteTest}
import it.pagopa.interop.commons.cqrs.service.{MongoDbReadModelService, ReadModelService}
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig
import org.scalamock.scalatest.MockFactory
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext

class UserApiAuthzSpec extends AnyWordSpecLike with MockFactory with AuthzScalatestRouteTest {

  val fakeAgreementManagementService: AgreementManagementService         = new FakeAgreementManagementService()
  val fakeCatalogManagementService: CatalogManagementService             = new FakeCatalogManagementService()
  val fakeAuthorizationManagementService: AuthorizationManagementService = new FakeAuthorizationManagementService()
  val fakeSelfcareV2ClientService: SelfcareV2ClientService               = new FakeSelfcareV2ClientService()
  val fakePurposeManagementService: PurposeManagementService             = new FakePurposeManagementService()
  implicit val fakeReadModel: ReadModelService                           = new MongoDbReadModelService(
    ReadModelConfig(
      "mongodb://localhost/?socketTimeoutMS=1&serverSelectionTimeoutMS=1&connectTimeoutMS=1&&autoReconnect=false&keepAlive=false",
      "db"
    )
  )

  val service: UserApiServiceImpl =
    UserApiServiceImpl(fakeAuthorizationManagementService, fakeSelfcareV2ClientService)(
      ExecutionContext.global,
      fakeReadModel
    )

  "User api authorization spec" should {
    "accept authorized roles for getClientUserKeys" in {
      val endpoint = AuthorizedRoutes.endpoints("getClientUserKeys")
      validateAuthorization(
        endpoint,
        { implicit c: Seq[(String, String)] => service.getClientUserKeys("test", "test") }
      )
    }
  }
}
