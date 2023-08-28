package it.pagopa.interop.authorizationprocess.authz

import it.pagopa.interop.authorizationprocess.api.impl.ClientApiMarshallerImpl._
import it.pagopa.interop.authorizationprocess.api.impl.ClientApiServiceImpl
import it.pagopa.interop.authorizationprocess.model.{ClientSeed, PurposeAdditionDetails}
import it.pagopa.interop.authorizationprocess.service._
import it.pagopa.interop.authorizationprocess.util.FakeDependencies._
import it.pagopa.interop.authorizationprocess.util.{AuthorizedRoutes, AuthzScalatestRouteTest}
import it.pagopa.interop.commons.cqrs.service.{MongoDbReadModelService, ReadModelService}
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import org.scalamock.scalatest.MockFactory
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID
import scala.concurrent.ExecutionContext

class ClientOperationAuthzSpec extends AnyWordSpecLike with MockFactory with AuthzScalatestRouteTest {

  val fakeAgreementManagementService: AgreementManagementService         = new FakeAgreementManagementService()
  val fakeCatalogManagementService: CatalogManagementService             = new FakeCatalogManagementService()
  val fakeAuthorizationManagementService: AuthorizationManagementService = new FakeAuthorizationManagementService()
  val fakePartyManagementService: PartyManagementService                 = new FakePartyManagementService()
  val fakePurposeManagementService: PurposeManagementService             = new FakePurposeManagementService()
  val fakeUserRegistryManagementService: UserRegistryManagementService   = new FakeUserRegistryManagementService()
  val fakeTenantManagementService: TenantManagementService               = new FakeTenantManagementService()
  val fakeDateTimeSupplier: OffsetDateTimeSupplier                       = () => OffsetDateTime.now(ZoneOffset.UTC)
  implicit val fakeReadModel: ReadModelService                           = new MongoDbReadModelService(
    ReadModelConfig(
      "mongodb://localhost/?socketTimeoutMS=1&serverSelectionTimeoutMS=1&connectTimeoutMS=1&&autoReconnect=false&keepAlive=false",
      "db"
    )
  )

  val service: ClientApiServiceImpl = ClientApiServiceImpl(
    fakeAuthorizationManagementService,
    fakeAgreementManagementService,
    fakeCatalogManagementService,
    fakePartyManagementService,
    fakePurposeManagementService,
    fakeUserRegistryManagementService,
    fakeTenantManagementService,
    fakeDateTimeSupplier
  )(ExecutionContext.global, fakeReadModel)

  "Client operation authorization spec" should {
    "accept authorized roles for delete Client" in {
      val endpoint = AuthorizedRoutes.endpoints("deleteClient")
      validateAuthorization(endpoint, { implicit c: Seq[(String, String)] => service.deleteClient("test") })
    }

    "accept authorized roles for createConsumerClient" in {
      val endpoint   = AuthorizedRoutes.endpoints("createConsumerClient")
      val clientSeed = ClientSeed(name = "pippo", None, Seq.empty)
      validateAuthorization(endpoint, { implicit c: Seq[(String, String)] => service.createConsumerClient(clientSeed) })
    }
    "accept authorized roles for createApiClient" in {
      val endpoint   = AuthorizedRoutes.endpoints("createApiClient")
      val clientSeed = ClientSeed(name = "pippo", None, Seq.empty)
      validateAuthorization(endpoint, { implicit c: Seq[(String, String)] => service.createApiClient(clientSeed) })
    }

    "accept authorized roles for getClient" in {
      val endpoint = AuthorizedRoutes.endpoints("getClient")
      validateAuthorization(endpoint, { implicit c: Seq[(String, String)] => service.getClient("fake") })
    }
    "accept authorized roles for getClients" in {
      val endpoint = AuthorizedRoutes.endpoints("getClients")
      validateAuthorization(
        endpoint,
        { implicit c: Seq[(String, String)] =>
          service.getClients(Some("name"), "relationshipIds", "consumerId", Some("purposeId"), None, 0, 0)
        }
      )
    }

    "accept authorized roles for getClientsWithKeys" in {
      val endpoint = AuthorizedRoutes.endpoints("getClientsWithKeys")
      validateAuthorization(
        endpoint,
        { implicit c: Seq[(String, String)] =>
          service.getClientsWithKeys(Some("name"), "relationshipIds", "consumerId", Some("purposeId"), None, 0, 0)
        }
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
      validateAuthorization(endpoint, { implicit c: Seq[(String, String)] => service.getClientKeys("yada", "yada") })
    }
    "accept authorized roles for getClientOperators" in {
      val endpoint = AuthorizedRoutes.endpoints("getClientOperators")
      validateAuthorization(endpoint, { implicit c: Seq[(String, String)] => service.getClientOperators("yada") })
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
    "accept authorized roles for removeArchivedPurpose" in {
      val endpoint = AuthorizedRoutes.endpoints("removeArchivedPurpose")
      validateAuthorization(endpoint, { implicit c: Seq[(String, String)] => service.removePurposeFromClients("yada") })
    }
  }
}
