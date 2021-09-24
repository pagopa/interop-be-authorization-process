package it.pagopa.pdnd.interop.uservice.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.model.AgreementEnums
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.ClientApiServiceImpl
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.util.SpecUtils
import it.pagopa.pdnd.interop.uservice.catalogmanagement.client.model.EServiceDescriptorEnums
import it.pagopa.pdnd.interop.uservice.{catalogmanagement, keymanagement}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class ClientOperationSpec extends AnyWordSpecLike with MockFactory with SpecUtils with ScalatestRouteTest {
  import clientApiMarshaller._

  val service = new ClientApiServiceImpl(
    mockAuthorizationManagementService,
    mockAgreementManagementService,
    mockCatalogManagementService,
    mockPartyManagementService
  )(ExecutionContext.global)

  "Client creation" should {
    "succeed" in {
      (mockCatalogManagementService.getEService _)
        .expects(bearerToken, clientSeed.eServiceId.toString)
        .once()
        .returns(Future.successful(eService))

      (mockPartyManagementService.getOrganizationByInstitutionId _)
        .expects(clientSeed.consumerInstitutionId)
        .once()
        .returns(Future.successful(organization))

      (mockAuthorizationManagementService.createClient _)
        .expects(clientSeed.eServiceId, UUID.fromString(organization.partyId), clientSeed.name, clientSeed.description)
        .once()
        .returns(Future.successful(client))

      mockClientComposition(withOperators = false)

      val expected = Client(
        id = client.id,
        eservice = EService(
          eService.id,
          eService.name,
          Organization(organization.institutionId, organization.description),
          Some(Descriptor(activeDescriptor.id, activeDescriptor.status.toString, activeDescriptor.version))
        ),
        consumer = Organization(consumer.institutionId, consumer.description),
        agreement = Agreement(
          agreement.id,
          agreement.status.toString,
          Descriptor(activeDescriptor.id, activeDescriptor.status.toString, activeDescriptor.version)
        ),
        name = client.name,
        description = client.description,
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
        .expects(bearerToken, clientSeed.eServiceId.toString)
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
            Some(Descriptor(activeDescriptor.id, activeDescriptor.status.toString, activeDescriptor.version))
          ),
          consumer = Organization(consumer.institutionId, consumer.description),
          agreement = Agreement(
            agreement.id,
            agreement.status.toString,
            Descriptor(activeDescriptor.id, activeDescriptor.status.toString, activeDescriptor.version)
          ),
          name = client.name,
          description = client.description,
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
        activeDescriptor.copy(id = descriptorId1, version = "1", status = EServiceDescriptorEnums.Status.Deprecated)
      val descriptor2 =
        activeDescriptor.copy(id = descriptorId2, version = "2", status = EServiceDescriptorEnums.Status.Deprecated)

      val eService1 = eService.copy(descriptors = Seq(descriptor1, descriptor2))

      val agreement1 =
        agreement.copy(id = UUID.randomUUID(), descriptorId = descriptorId1, status = AgreementEnums.Status.Suspended)
      val agreement2 =
        agreement.copy(id = UUID.randomUUID(), descriptorId = descriptorId2, status = AgreementEnums.Status.Suspended)

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
            agreement2.status.toString,
            Descriptor(descriptor2.id, descriptor2.status.toString, descriptor2.version)
          ),
          name = client.name,
          description = client.description,
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

      (mockAuthorizationManagementService.listClients _)
        .expects(*, *, *, *)
        .once()
        .returns(Future.successful(Seq(client)))

      (mockCatalogManagementService.getEService _)
        .expects(*, client.eServiceId.toString)
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
        .expects(*, client.consumerId.toString, client.eServiceId.toString, None)
        .once()
        .returns(Future.successful(Seq(agreement)))

      val expected = Seq(
        Client(
          id = client.id,
          eservice = EService(
            eService.id,
            eService.name,
            Organization(organization.institutionId, organization.description),
            Some(Descriptor(activeDescriptor.id, activeDescriptor.status.toString, activeDescriptor.version))
          ),
          consumer = Organization(consumer.institutionId, consumer.description),
          agreement = Agreement(
            agreement.id,
            agreement.status.toString,
            Descriptor(activeDescriptor.id, activeDescriptor.status.toString, activeDescriptor.version)
          ),
          name = client.name,
          description = client.description,
          operators = Some(Seq.empty)
        )
      )

      Get() ~> service.listClients(Some(0), Some(10), Some(eServiceId.toString), None) ~> check {
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
}
