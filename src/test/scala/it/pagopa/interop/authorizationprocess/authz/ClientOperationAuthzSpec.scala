package it.pagopa.interop.authorizationprocess.authz

import it.pagopa.interop.authorizationprocess.api.impl.ClientApiMarshallerImpl._
import it.pagopa.interop.authorizationprocess.api.impl.ClientApiServiceImpl
import it.pagopa.interop.authorizationprocess.model.{ClientSeed, PurposeAdditionDetails}
import it.pagopa.interop.authorizationprocess.service._
import it.pagopa.interop.authorizationprocess.util.FakeDependencies._
import it.pagopa.interop.authorizationprocess.util.{AuthorizedRoutes, AuthzScalatestRouteTest}
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
      validateAuthorization(endpoint, { implicit c: Seq[(String, String)] => service.deleteClient("test") })
    }

    "accept authorized roles for createConsumerClient" in {
      val endpoint   = AuthorizedRoutes.endpoints("createConsumerClient")
      val clientSeed = ClientSeed(name = "pippo")
      validateAuthorization(endpoint, { implicit c: Seq[(String, String)] => service.createConsumerClient(clientSeed) })
    }
    "accept authorized roles for createApiClient" in {
      val endpoint   = AuthorizedRoutes.endpoints("createApiClient")
      val clientSeed = ClientSeed(name = "pippo")
      validateAuthorization(endpoint, { implicit c: Seq[(String, String)] => service.createApiClient(clientSeed) })
    }

    "accept authorized roles for getClient" in {
      val endpoint = AuthorizedRoutes.endpoints("getClient")
      validateAuthorization(endpoint, { implicit c: Seq[(String, String)] => service.getClient("fake") })
    }
    "accept authorized roles for listClients" in {
      val endpoint = AuthorizedRoutes.endpoints("listClients")
      validateAuthorization(
        endpoint,
        { implicit c: Seq[(String, String)] => service.listClients(None, None, "test", None, None) }
      )
    }
    "accept authorized roles for clientOperatorRelationshipBinding" in {
      val endpoint = AuthorizedRoutes.endpoints("clientOperatorRelationshipBinding")
      validateAuthorization(
        endpoint,
        { implicit c: Seq[(String, String)] => service.clientOperatorRelationshipBinding("yada", "yada") }
      )
    }
    "accept authorized roles for removeClientOperatorRelationship" in {
      val endpoint = AuthorizedRoutes.endpoints("removeClientOperatorRelationship")
      validateAuthorization(
        endpoint,
        { implicit c: Seq[(String, String)] => service.removeClientOperatorRelationship("yada", "yada") }
      )
    }
    "accept authorized roles for getClientKeyById" in {
      val endpoint = AuthorizedRoutes.endpoints("getClientKeyById")
      validateAuthorization(endpoint, { implicit c: Seq[(String, String)] => service.getClientKeyById("yada", "yada") })
    }
    "accept authorized roles for deleteClientKeyById" in {
      val endpoint = AuthorizedRoutes.endpoints("deleteClientKeyById")
      validateAuthorization(
        endpoint,
        { implicit c: Seq[(String, String)] => service.deleteClientKeyById("yada", "yada") }
      )
    }

    "accept authorized roles for createKeys" in {
      val endpoint = AuthorizedRoutes.endpoints("createKeys")
      validateAuthorization(endpoint, { implicit c: Seq[(String, String)] => service.createKeys("yada", Seq.empty) })
    }
    "accept authorized roles for getClientKeys" in {
      val endpoint = AuthorizedRoutes.endpoints("getClientKeys")
      validateAuthorization(endpoint, { implicit c: Seq[(String, String)] => service.getClientKeys("yada") })
    }
    "accept authorized roles for getClientOperators" in {
      val endpoint = AuthorizedRoutes.endpoints("getClientOperators")
      validateAuthorization(endpoint, { implicit c: Seq[(String, String)] => service.getClientOperators("yada") })
    }
    "accept authorized roles for getClientOperatorRelationshipById" in {
      val endpoint = AuthorizedRoutes.endpoints("getClientOperatorRelationshipById")
      validateAuthorization(
        endpoint,
        { implicit c: Seq[(String, String)] => service.getClientOperatorRelationshipById("yada", "yada") }
      )
    }
    "accept authorized roles for addClientPurpose" in {
      val endpoint = AuthorizedRoutes.endpoints("addClientPurpose")
      val details  = PurposeAdditionDetails(UUID.randomUUID())
      validateAuthorization(
        endpoint,
        { implicit c: Seq[(String, String)] => service.addClientPurpose("yada", details) }
      )
    }
    "accept authorized roles for removeClientPurpose" in {
      val endpoint = AuthorizedRoutes.endpoints("removeClientPurpose")
      validateAuthorization(
        endpoint,
        { implicit c: Seq[(String, String)] => service.removeClientPurpose("yada", "test") }
      )
    }
    "accept authorized roles for getEncodedClientKeyById" in {
      val endpoint = AuthorizedRoutes.endpoints("getEncodedClientKeyById")
      validateAuthorization(
        endpoint,
        { implicit c: Seq[(String, String)] => service.getEncodedClientKeyById("yada", "test") }
      )
    }
  }
}
