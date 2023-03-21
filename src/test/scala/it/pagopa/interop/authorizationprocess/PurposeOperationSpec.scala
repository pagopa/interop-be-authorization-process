package it.pagopa.interop.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.agreementmanagement.client.{model => AgreementManagementDependency}
import it.pagopa.interop.authorizationmanagement.client.{model => AuthorizationManagementDependency}
import it.pagopa.interop.authorizationprocess.api.impl.ClientApiServiceImpl
import it.pagopa.interop.authorizationprocess.api.impl.ClientApiMarshallerImpl._
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationprocess.util.SpecUtilsWithImplicit
import it.pagopa.interop.catalogmanagement.client.{model => CatalogManagementDependency}

import it.pagopa.interop.purposemanagement.client.{model => PurposeManagementDependency}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors._

class PurposeOperationSpec extends AnyWordSpecLike with MockFactory with SpecUtilsWithImplicit with ScalatestRouteTest {

  val service: ClientApiServiceImpl = ClientApiServiceImpl(
    mockAuthorizationManagementService,
    mockAgreementManagementService,
    mockCatalogManagementService,
    mockPartyManagementService,
    mockPurposeManagementService,
    mockUserRegistryManagementService,
    mockTenantManagementService,
    mockReadModel
  )(ExecutionContext.global)

  "Purpose add to Client" should {
    "succeed" in {

      val purposeSeed = AuthorizationManagementDependency.PurposeSeed(states =
        AuthorizationManagementDependency.ClientStatesChainSeed(
          eservice = AuthorizationManagementDependency.ClientEServiceDetailsSeed(
            eserviceId = eService.id,
            descriptorId = activeDescriptor.id,
            state = AuthorizationManagementDependency.ClientComponentState.ACTIVE,
            audience = eService.descriptors.head.audience,
            voucherLifespan = eService.descriptors.head.voucherLifespan
          ),
          agreement = AuthorizationManagementDependency.ClientAgreementDetailsSeed(
            eserviceId = agreement.eserviceId,
            consumerId = agreement.consumerId,
            agreementId = agreement.id,
            state = AuthorizationManagementDependency.ClientComponentState.INACTIVE
          ),
          purpose = AuthorizationManagementDependency.ClientPurposeDetailsSeed(
            purposeId = purpose.id,
            versionId = purposeVersion.id,
            state = AuthorizationManagementDependency.ClientComponentState.ACTIVE
          )
        )
      )

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(client.id, *)
        .once()
        .returns(Future.successful(client.copy(consumerId = consumerId)))

      (mockPurposeManagementService
        .getPurpose(_: UUID)(_: Seq[(String, String)]))
        .expects(purpose.id, *)
        .once()
        .returns(
          Future.successful(
            purpose
              .copy(
                consumerId = consumerId,
                versions = Seq(purposeVersion.copy(state = PurposeManagementDependency.PurposeVersionState.ACTIVE))
              )
          )
        )

      (mockCatalogManagementService
        .getEService(_: UUID)(_: Seq[(String, String)]))
        .expects(eService.id, *)
        .once()
        .returns(
          Future.successful(
            eService.copy(descriptors =
              Seq(activeDescriptor.copy(state = CatalogManagementDependency.EServiceDescriptorState.PUBLISHED))
            )
          )
        )

      (mockAgreementManagementService
        .getAgreements(_: UUID, _: UUID)(_: Seq[(String, String)]))
        .expects(eService.id, consumer.id, *)
        .once()
        .returns(Future.successful(Seq(agreement.copy(state = AgreementManagementDependency.AgreementState.SUSPENDED))))

      (mockAuthorizationManagementService
        .addClientPurpose(_: UUID, _: AuthorizationManagementDependency.PurposeSeed)(_: Seq[(String, String)]))
        .expects(client.id, purposeSeed, *)
        .once()
        .returns(Future.successful(clientPurpose))

      Get() ~> service.addClientPurpose(client.id.toString, PurposeAdditionDetails(purpose.id)) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if the caller is not the client consumer" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(client.id, *)
        .once()
        .returns(Future.successful(client.copy(consumerId = UUID.randomUUID())))

      Get() ~> service.addClientPurpose(client.id.toString, PurposeAdditionDetails(purpose.id)) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }

    "fail if the caller is not the purpose consumer" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(client.id, *)
        .once()
        .returns(Future.successful(client))

      (mockPurposeManagementService
        .getPurpose(_: UUID)(_: Seq[(String, String)]))
        .expects(purpose.id, *)
        .once()
        .returns(
          Future.successful(
            purpose
              .copy(
                consumerId = UUID.randomUUID,
                versions = Seq(purposeVersion.copy(state = PurposeManagementDependency.PurposeVersionState.ACTIVE))
              )
          )
        )

      Get() ~> service.addClientPurpose(client.id.toString, PurposeAdditionDetails(purpose.id)) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }

    "fail if no valid agreement exists" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(client.id, *)
        .once()
        .returns(Future.successful(client.copy(consumerId = consumerId)))

      (mockPurposeManagementService
        .getPurpose(_: UUID)(_: Seq[(String, String)]))
        .expects(purpose.id, *)
        .once()
        .returns(
          Future.successful(
            purpose
              .copy(
                consumerId = consumerId,
                versions = Seq(purposeVersion.copy(state = PurposeManagementDependency.PurposeVersionState.ACTIVE))
              )
          )
        )

      (mockCatalogManagementService
        .getEService(_: UUID)(_: Seq[(String, String)]))
        .expects(eService.id, *)
        .once()
        .returns(
          Future.successful(
            eService.copy(descriptors =
              Seq(activeDescriptor.copy(state = CatalogManagementDependency.EServiceDescriptorState.PUBLISHED))
            )
          )
        )

      (mockAgreementManagementService
        .getAgreements(_: UUID, _: UUID)(_: Seq[(String, String)]))
        .expects(eService.id, consumer.id, *)
        .once()
        .returns(Future.successful(Seq(agreement.copy(state = AgreementManagementDependency.AgreementState.PENDING))))

      Get() ~> service.addClientPurpose(client.id.toString, PurposeAdditionDetails(purpose.id)) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[Problem].errors.head.code shouldEqual "007-0005"
      }
    }

    "fail if Purpose does not exist" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(client.id, *)
        .once()
        .returns(Future.successful(client.copy(consumerId = consumerId)))

      (mockPurposeManagementService
        .getPurpose(_: UUID)(_: Seq[(String, String)]))
        .expects(purpose.id, *)
        .once()
        .returns(Future.failed(PurposeNotFound(purpose.id)))

      Get() ~> service.addClientPurpose(client.id.toString, PurposeAdditionDetails(purpose.id)) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "succeed even if Purpose has only draft versions" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(client.id, *)
        .once()
        .returns(Future.successful(client.copy(consumerId = consumerId)))

      (mockPurposeManagementService
        .getPurpose(_: UUID)(_: Seq[(String, String)]))
        .expects(purpose.id, *)
        .once()
        .returns(
          Future.successful(
            purpose
              .copy(
                consumerId = consumerId,
                versions = Seq(purposeVersion.copy(state = PurposeManagementDependency.PurposeVersionState.DRAFT))
              )
          )
        )

      (mockCatalogManagementService
        .getEService(_: UUID)(_: Seq[(String, String)]))
        .expects(eService.id, *)
        .once()
        .returns(
          Future.successful(
            eService.copy(descriptors =
              Seq(activeDescriptor.copy(state = CatalogManagementDependency.EServiceDescriptorState.PUBLISHED))
            )
          )
        )

      (mockAgreementManagementService
        .getAgreements(_: UUID, _: UUID)(_: Seq[(String, String)]))
        .expects(eService.id, consumer.id, *)
        .once()
        .returns(Future.successful(Seq(agreement.copy(state = AgreementManagementDependency.AgreementState.ACTIVE))))

      (mockAuthorizationManagementService
        .addClientPurpose(_: UUID, _: AuthorizationManagementDependency.PurposeSeed)(_: Seq[(String, String)]))
        .expects(client.id, *, *)
        .once()
        .returns(Future.successful(clientPurpose))

      Post() ~> service.addClientPurpose(client.id.toString, PurposeAdditionDetails(purpose.id)) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if Purpose has only archived versions" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: Seq[(String, String)]))
        .expects(client.id, *)
        .once()
        .returns(Future.successful(client.copy(consumerId = consumerId)))

      (mockPurposeManagementService
        .getPurpose(_: UUID)(_: Seq[(String, String)]))
        .expects(purpose.id, *)
        .once()
        .returns(
          Future.successful(
            purpose
              .copy(
                consumerId = consumerId,
                versions = Seq(purposeVersion.copy(state = PurposeManagementDependency.PurposeVersionState.ARCHIVED))
              )
          )
        )

      (mockCatalogManagementService
        .getEService(_: UUID)(_: Seq[(String, String)]))
        .expects(eService.id, *)
        .once()
        .returns(
          Future.successful(
            eService.copy(descriptors =
              Seq(activeDescriptor.copy(state = CatalogManagementDependency.EServiceDescriptorState.PUBLISHED))
            )
          )
        )

      (mockAgreementManagementService
        .getAgreements(_: UUID, _: UUID)(_: Seq[(String, String)]))
        .expects(eService.id, consumer.id, *)
        .once()
        .returns(Future.successful(Seq(agreement.copy(state = AgreementManagementDependency.AgreementState.ACTIVE))))

      Post() ~> service.addClientPurpose(client.id.toString, PurposeAdditionDetails(purpose.id)) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }

  }

}
