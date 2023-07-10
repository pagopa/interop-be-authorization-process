package it.pagopa.interop.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.authorizationmanagement.client.{model => AuthorizationManagementDependency}
import it.pagopa.interop.authorizationprocess.api.impl.ClientApiServiceImpl
import it.pagopa.interop.authorizationprocess.api.impl.ClientApiMarshallerImpl._
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationprocess.util.SpecUtilsWithImplicit
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors._
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.catalogmanagement.model.Published
import it.pagopa.interop.purposemanagement.model.purpose.{Draft, Active => ActivePurpose, Archived}
import it.pagopa.interop.agreementmanagement.model.agreement.{Pending, Active, Suspended}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
class PurposeOperationSpec extends AnyWordSpecLike with MockFactory with SpecUtilsWithImplicit with ScalatestRouteTest {

  val service: ClientApiServiceImpl = ClientApiServiceImpl(
    mockAuthorizationManagementService,
    mockAgreementManagementService,
    mockCatalogManagementService,
    mockPartyManagementService,
    mockPurposeManagementService,
    mockUserRegistryManagementService,
    mockTenantManagementService,
    mockDateTimeSupplier
  )(ExecutionContext.global, mockReadModel)

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
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient.copy(consumerId = consumerId)))

      (mockPurposeManagementService
        .getPurposeById(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(purpose.id, *, *)
        .once()
        .returns(
          Future.successful(
            purpose
              .copy(consumerId = consumerId, versions = Seq(purposeVersion.copy(state = ActivePurpose)))
          )
        )

      (mockCatalogManagementService
        .getEServiceById(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(eService.id, *, *)
        .once()
        .returns(Future.successful(eService.copy(descriptors = Seq(activeDescriptor.copy(state = Published)))))

      (mockAgreementManagementService
        .getAgreements(_: UUID, _: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(eService.id, consumer.id, *, *)
        .once()
        .returns(Future.successful(Seq(agreement.copy(state = Suspended))))

      (mockAuthorizationManagementService
        .addClientPurpose(_: UUID, _: AuthorizationManagementDependency.PurposeSeed)(_: Seq[(String, String)]))
        .expects(persistentClient.id, purposeSeed, *)
        .once()
        .returns(Future.successful(clientPurpose))

      Get() ~> service.addClientPurpose(persistentClient.id.toString, PurposeAdditionDetails(purpose.id)) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if the caller is not the client consumer" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient.copy(consumerId = UUID.randomUUID())))

      Get() ~> service.addClientPurpose(persistentClient.id.toString, PurposeAdditionDetails(purpose.id)) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }

    "fail if the caller is not the purpose consumer" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient))

      (mockPurposeManagementService
        .getPurposeById(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(purpose.id, *, *)
        .once()
        .returns(
          Future.successful(
            purpose
              .copy(consumerId = UUID.randomUUID, versions = Seq(purposeVersion.copy(state = ActivePurpose)))
          )
        )

      Get() ~> service.addClientPurpose(persistentClient.id.toString, PurposeAdditionDetails(purpose.id)) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }

    "fail if no valid agreement exists" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient.copy(consumerId = consumerId)))

      (mockPurposeManagementService
        .getPurposeById(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(purpose.id, *, *)
        .once()
        .returns(
          Future.successful(
            purpose
              .copy(consumerId = consumerId, versions = Seq(purposeVersion.copy(state = ActivePurpose)))
          )
        )

      (mockCatalogManagementService
        .getEServiceById(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(eService.id, *, *)
        .once()
        .returns(Future.successful(eService.copy(descriptors = Seq(activeDescriptor.copy(state = Published)))))

      (mockAgreementManagementService
        .getAgreements(_: UUID, _: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(eService.id, consumer.id, *, *)
        .once()
        .returns(Future.successful(Seq(agreement.copy(state = Pending))))

      Get() ~> service.addClientPurpose(persistentClient.id.toString, PurposeAdditionDetails(purpose.id)) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[Problem].errors.head.code shouldEqual "007-0005"
      }
    }

    "fail if Purpose does not exist" in {
      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient.copy(consumerId = consumerId)))

      (mockPurposeManagementService
        .getPurposeById(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(purpose.id, *, *)
        .once()
        .returns(Future.failed(PurposeNotFound(purpose.id)))

      Get() ~> service.addClientPurpose(persistentClient.id.toString, PurposeAdditionDetails(purpose.id)) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "succeed even if Purpose has only draft versions" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient.copy(consumerId = consumerId)))

      (mockPurposeManagementService
        .getPurposeById(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(purpose.id, *, *)
        .once()
        .returns(
          Future.successful(
            purpose
              .copy(consumerId = consumerId, versions = Seq(purposeVersion.copy(state = Draft)))
          )
        )

      (mockCatalogManagementService
        .getEServiceById(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(eService.id, *, *)
        .once()
        .returns(Future.successful(eService.copy(descriptors = Seq(activeDescriptor.copy(state = Published)))))

      (mockAgreementManagementService
        .getAgreements(_: UUID, _: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(eService.id, consumer.id, *, *)
        .once()
        .returns(Future.successful(Seq(agreement.copy(state = Active))))

      (mockAuthorizationManagementService
        .addClientPurpose(_: UUID, _: AuthorizationManagementDependency.PurposeSeed)(_: Seq[(String, String)]))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(clientPurpose))

      Post() ~> service.addClientPurpose(persistentClient.id.toString, PurposeAdditionDetails(purpose.id)) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if Purpose has only archived versions" in {

      (mockAuthorizationManagementService
        .getClient(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(persistentClient.id, *, *)
        .once()
        .returns(Future.successful(persistentClient.copy(consumerId = consumerId)))

      (mockPurposeManagementService
        .getPurposeById(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(purpose.id, *, *)
        .once()
        .returns(
          Future.successful(
            purpose
              .copy(consumerId = consumerId, versions = Seq(purposeVersion.copy(state = Archived)))
          )
        )

      (mockCatalogManagementService
        .getEServiceById(_: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(eService.id, *, *)
        .once()
        .returns(Future.successful(eService.copy(descriptors = Seq(activeDescriptor.copy(state = Published)))))

      (mockAgreementManagementService
        .getAgreements(_: UUID, _: UUID)(_: ExecutionContext, _: ReadModelService))
        .expects(eService.id, consumer.id, *, *)
        .once()
        .returns(Future.successful(Seq(agreement.copy(state = Active))))

      Post() ~> service.addClientPurpose(persistentClient.id.toString, PurposeAdditionDetails(purpose.id)) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }
}
