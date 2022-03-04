package it.pagopa.interop.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.interop.agreementmanagement.client.{model => AgreementManagementDependency}
import it.pagopa.interop.authorizationmanagement.client.{model => AuthorizationManagementDependency}
import it.pagopa.interop.authorizationprocess.api.impl.ClientApiServiceImpl
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationprocess.util.SpecUtils
import it.pagopa.interop.catalogmanagement.client.{model => CatalogManagementDependency}
import it.pagopa.interop.purposemanagement
import it.pagopa.interop.purposemanagement.client.{model => PurposeManagementDependency}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class PurposeOperationSpec extends AnyWordSpecLike with MockFactory with SpecUtils with ScalatestRouteTest {

  import clientApiMarshaller._

  val service: ClientApiServiceImpl = ClientApiServiceImpl(
    mockAuthorizationManagementService,
    mockAgreementManagementService,
    mockCatalogManagementService,
    mockPartyManagementService,
    mockPurposeManagementService,
    mockUserRegistryManagementService
  )(ExecutionContext.global)

  "Purpose add to Client" should {
    "succeed" in {
      val purposeSeed = AuthorizationManagementDependency.PurposeSeed(
        purposeId = purpose.id,
        states = AuthorizationManagementDependency.ClientStatesChainSeed(
          eservice = AuthorizationManagementDependency.ClientEServiceDetailsSeed(
            eserviceId = eService.id,
            state = AuthorizationManagementDependency.ClientComponentState.ACTIVE,
            audience = eService.descriptors.head.audience,
            voucherLifespan = eService.descriptors.head.voucherLifespan
          ),
          agreement = AuthorizationManagementDependency.ClientAgreementDetailsSeed(
            eserviceId = agreement.eserviceId,
            consumerId = agreement.consumerId,
            state = AuthorizationManagementDependency.ClientComponentState.INACTIVE
          ),
          purpose = AuthorizationManagementDependency.ClientPurposeDetailsSeed(
            purposeId = purpose.id,
            state = AuthorizationManagementDependency.ClientComponentState.ACTIVE
          )
        )
      )

      (mockPurposeManagementService
        .getPurpose(_: String)(_: UUID))
        .expects(bearerToken, purpose.id)
        .once()
        .returns(
          Future.successful(
            purpose
              .copy(versions = Seq(purposeVersion.copy(state = PurposeManagementDependency.PurposeVersionState.ACTIVE)))
          )
        )

      (mockCatalogManagementService
        .getEService(_: String)(_: UUID))
        .expects(bearerToken, eService.id)
        .once()
        .returns(
          Future.successful(
            eService.copy(descriptors =
              Seq(activeDescriptor.copy(state = CatalogManagementDependency.EServiceDescriptorState.PUBLISHED))
            )
          )
        )

      (mockAgreementManagementService
        .getAgreements(_: String)(_: UUID, _: UUID))
        .expects(bearerToken, eService.id, consumer.id)
        .once()
        .returns(Future.successful(Seq(agreement.copy(state = AgreementManagementDependency.AgreementState.SUSPENDED))))

      (mockAuthorizationManagementService
        .addClientPurpose(_: UUID, _: AuthorizationManagementDependency.PurposeSeed)(_: String))
        .expects(client.id, purposeSeed, bearerToken)
        .once()
        .returns(Future.successful(clientPurpose))

      Get() ~> service.addClientPurpose(client.id.toString, PurposeAdditionDetails(purpose.id)) ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "fail if no valid agreement exists" in {
      (mockPurposeManagementService
        .getPurpose(_: String)(_: UUID))
        .expects(bearerToken, purpose.id)
        .once()
        .returns(
          Future.successful(
            purpose
              .copy(versions = Seq(purposeVersion.copy(state = PurposeManagementDependency.PurposeVersionState.ACTIVE)))
          )
        )

      (mockCatalogManagementService
        .getEService(_: String)(_: UUID))
        .expects(bearerToken, eService.id)
        .once()
        .returns(
          Future.successful(
            eService.copy(descriptors =
              Seq(activeDescriptor.copy(state = CatalogManagementDependency.EServiceDescriptorState.PUBLISHED))
            )
          )
        )

      (mockAgreementManagementService
        .getAgreements(_: String)(_: UUID, _: UUID))
        .expects(bearerToken, eService.id, consumer.id)
        .once()
        .returns(Future.successful(Seq(agreement.copy(state = AgreementManagementDependency.AgreementState.PENDING))))

      Get() ~> service.addClientPurpose(client.id.toString, PurposeAdditionDetails(purpose.id)) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[Problem].errors.head.code shouldEqual "007-0045"
      }
    }

    "fail if Purpose does not exist" in {
      (mockPurposeManagementService
        .getPurpose(_: String)(_: UUID))
        .expects(bearerToken, purpose.id)
        .once()
        .returns(Future.failed(purposemanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.addClientPurpose(client.id.toString, PurposeAdditionDetails(purpose.id)) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

  }

}
