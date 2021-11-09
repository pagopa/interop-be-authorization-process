package it.pagopa.pdnd.interop.uservice.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.{model => AgreementManagementDependency}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.ClientApiServiceImpl
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.AgreementManagementService.agreementStateToApi
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.AuthorizationManagementService.clientStateToApi
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.CatalogManagementService.descriptorStateToApi
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.PartyManagementService
import it.pagopa.pdnd.interop.uservice.authorizationprocess.util.SpecUtils
import it.pagopa.pdnd.interop.uservice.catalogmanagement.client.{model => CatalogManagementDependency}
import it.pagopa.pdnd.interop.uservice.partymanagement.client.model.Relationships
import it.pagopa.pdnd.interop.uservice.{catalogmanagement, keymanagement}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class ClientOperationSpec extends AnyWordSpecLike with MockFactory with SpecUtils with ScalatestRouteTest {

  import clientApiMarshaller._

  val service: ClientApiServiceImpl = ClientApiServiceImpl(
    mockAuthorizationManagementService,
    mockAgreementManagementService,
    mockCatalogManagementService,
    mockPartyManagementService,
    mockUserRegistryManagementService
  )(ExecutionContext.global)

  "Client creation" should {
    "succeed" in {
      (mockCatalogManagementService.getEService _)
        .expects(bearerToken, clientSeed.eServiceId)
        .once()
        .returns(Future.successful(eService))

      (mockAuthorizationManagementService.createClient _)
        .expects(clientSeed.eServiceId, organization.id, clientSeed.name, clientSeed.purposes, clientSeed.description)
        .once()
        .returns(Future.successful(client))

      mockClientComposition(withOperators = false)

      val expected = Client(
        id = client.id,
        eservice = EService(
          eService.id,
          eService.name,
          Organization(organization.institutionId, organization.description),
          Some(Descriptor(activeDescriptor.id, descriptorStateToApi(activeDescriptor.state), activeDescriptor.version))
        ),
        consumer = Organization(consumer.institutionId, consumer.description),
        agreement = Agreement(
          agreement.id,
          agreementStateToApi(agreement.state),
          Descriptor(activeDescriptor.id, descriptorStateToApi(activeDescriptor.state), activeDescriptor.version)
        ),
        name = client.name,
        purposes = client.purposes,
        description = client.description,
        state = clientStateToApi(client.state),
        operators = Some(Seq.empty)
      )

      Get() ~> service.createClient(clientSeed) ~> check {
        status shouldEqual StatusCodes.Created
        entityAs[Client] shouldEqual expected
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      Get() ~> service.createClient(clientSeed) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if the E-Service does not exist" in {
      (mockCatalogManagementService.getEService _)
        .expects(bearerToken, clientSeed.eServiceId)
        .once()
        .returns(Future.failed(catalogmanagement.client.invoker.ApiError(404, "Some message", None)))

      Get() ~> service.createClient(clientSeed) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

  }

  "Client retrieve" should {
    "succeed" in {
      (mockAuthorizationManagementService.getClient _)
        .expects(*)
        .once()
        .returns(Future.successful(client))

      mockClientComposition(withOperators = false)

      val expected =
        Client(
          id = client.id,
          eservice = EService(
            eService.id,
            eService.name,
            Organization(organization.institutionId, organization.description),
            Some(
              Descriptor(activeDescriptor.id, descriptorStateToApi(activeDescriptor.state), activeDescriptor.version)
            )
          ),
          consumer = Organization(consumer.institutionId, consumer.description),
          agreement = Agreement(
            agreement.id,
            agreementStateToApi(agreement.state),
            Descriptor(activeDescriptor.id, descriptorStateToApi(activeDescriptor.state), activeDescriptor.version)
          ),
          name = client.name,
          purposes = client.purposes,
          description = client.description,
          state = clientStateToApi(client.state),
          operators = Some(Seq.empty)
        )

      Get() ~> service.getClient(client.id.toString) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Client] shouldEqual expected
      }
    }

    "succeed on multiple agreements" in {
      val descriptorId1 = UUID.randomUUID()
      val descriptorId2 = UUID.randomUUID()
      val descriptor1 =
        activeDescriptor.copy(
          id = descriptorId1,
          version = "1",
          state = CatalogManagementDependency.EServiceDescriptorState.DEPRECATED
        )
      val descriptor2 =
        activeDescriptor.copy(
          id = descriptorId2,
          version = "2",
          state = CatalogManagementDependency.EServiceDescriptorState.DEPRECATED
        )

      val eService1 = eService.copy(descriptors = Seq(descriptor1, descriptor2))

      val agreement1 =
        agreement.copy(
          id = UUID.randomUUID(),
          descriptorId = descriptorId1,
          state = AgreementManagementDependency.AgreementState.SUSPENDED
        )
      val agreement2 =
        agreement.copy(
          id = UUID.randomUUID(),
          descriptorId = descriptorId2,
          state = AgreementManagementDependency.AgreementState.SUSPENDED
        )

      (mockAuthorizationManagementService.getClient _)
        .expects(*)
        .once()
        .returns(Future.successful(client))

      mockClientComposition(withOperators = false, eService = eService1, agreements = Seq(agreement1, agreement2))

      val expected =
        Client(
          id = client.id,
          eservice = EService(
            eService1.id,
            eService1.name,
            Organization(organization.institutionId, organization.description),
            activeDescriptor = None
          ),
          consumer = Organization(consumer.institutionId, consumer.description),
          agreement = Agreement(
            agreement2.id,
            agreementStateToApi(agreement2.state),
            Descriptor(descriptor2.id, descriptorStateToApi(descriptor2.state), descriptor2.version)
          ),
          name = client.name,
          purposes = client.purposes,
          description = client.description,
          state = clientStateToApi(client.state),
          operators = Some(Seq.empty)
        )

      Get() ~> service.getClient(client.id.toString) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Client] shouldEqual expected
      }
    }

    "fail if client does not exist" in {
      (mockAuthorizationManagementService.getClient _)
        .expects(*)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.getClient(client.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Client list" should {
    "succeed" in {
      val offset           = Some(0)
      val limit            = Some(10)
      val eServiceUuid     = Some(eServiceId)
      val relationshipUuid = Some(relationship.id)
      val consumerUuid     = Some(client.consumerId)

      val eServiceIdStr = eServiceUuid.map(_.toString)
      val operatorId    = operator.id
      val consumerId    = consumer.id

      (mockPartyManagementService.getRelationshipsByPersonId _)
        .expects(operatorId, Some(PartyManagementService.ROLE_SECURITY_OPERATOR))
        .once()
        .returns(Future.successful(Relationships(Seq(relationship))))

      (mockAuthorizationManagementService.listClients _)
        .expects(offset, limit, eServiceUuid, relationshipUuid, consumerUuid)
        .once()
        .returns(Future.successful(Seq(client)))

      (mockCatalogManagementService.getEService _)
        .expects(*, client.eServiceId)
        .returns(Future.successful(eService))

      (mockPartyManagementService.getOrganization _)
        .expects(eService.producerId)
        .once()
        .returns(Future.successful(organization))

      (mockPartyManagementService.getOrganization _)
        .expects(client.consumerId)
        .once()
        .returns(Future.successful(consumer))

      (mockAgreementManagementService.getAgreements _)
        .expects(*, client.consumerId, client.eServiceId, None)
        .once()
        .returns(Future.successful(Seq(agreement)))

      val expected = Seq(
        Client(
          id = client.id,
          eservice = EService(
            eService.id,
            eService.name,
            Organization(organization.institutionId, organization.description),
            Some(
              Descriptor(activeDescriptor.id, descriptorStateToApi(activeDescriptor.state), activeDescriptor.version)
            )
          ),
          consumer = Organization(consumer.institutionId, consumer.description),
          agreement = Agreement(
            agreement.id,
            agreementStateToApi(agreement.state),
            Descriptor(activeDescriptor.id, descriptorStateToApi(activeDescriptor.state), activeDescriptor.version)
          ),
          name = client.name,
          purposes = client.purposes,
          description = client.description,
          state = clientStateToApi(client.state),
          operators = Some(Seq.empty)
        )
      )

      Get() ~> service.listClients(
        offset,
        limit,
        eServiceIdStr,
        Some(operatorId.toString),
        Some(consumerId.toString)
      ) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Seq[Client]] shouldEqual expected
      }
    }
  }

  "Client delete" should {
    "succeed" in {
      (mockAuthorizationManagementService.deleteClient _)
        .expects(*)
        .once()
        .returns(Future.successful(()))

      Get() ~> service.deleteClient(client.id.toString) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if client does not exist" in {
      (mockAuthorizationManagementService.deleteClient _)
        .expects(*)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.deleteClient(client.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Client activation" should {
    "succeed" in {
      (mockAuthorizationManagementService.activateClient _)
        .expects(*)
        .once()
        .returns(Future.successful(()))

      Get() ~> service.activateClientById(client.id.toString) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if client does not exist" in {
      (mockAuthorizationManagementService.activateClient _)
        .expects(*)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.activateClientById(client.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Client suspension" should {
    "succeed" in {
      (mockAuthorizationManagementService.suspendClient _)
        .expects(*)
        .once()
        .returns(Future.successful(()))

      Get() ~> service.suspendClientById(client.id.toString) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if client does not exist" in {
      (mockAuthorizationManagementService.suspendClient _)
        .expects(*)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.suspendClientById(client.id.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

}
