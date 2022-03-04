package it.pagopa.interop.authorizationprocess.service

import it.pagopa.interop.agreementmanagement.client.model.{Agreement, AgreementState}
import it.pagopa.interop.authorizationprocess.model.{
  Agreement => ApiAgreement,
  AgreementState => ApiAgreementState,
  EService => ApiEService,
  EServiceDescriptor => ApiEServiceDescriptor
}

import java.util.UUID
import scala.concurrent.Future

trait AgreementManagementService {
  def getAgreements(bearerToken: String)(eServiceId: UUID, consumerId: UUID): Future[Seq[Agreement]]

}

object AgreementManagementService {
  def agreementToApi(agreement: Agreement, eService: ApiEService, descriptor: ApiEServiceDescriptor): ApiAgreement =
    ApiAgreement(id = agreement.id, state = stateToApi(agreement.state), eservice = eService, descriptor = descriptor)

  def stateToApi(state: AgreementState): ApiAgreementState =
    state match {
      case AgreementState.ACTIVE    => ApiAgreementState.ACTIVE
      case AgreementState.PENDING   => ApiAgreementState.PENDING
      case AgreementState.SUSPENDED => ApiAgreementState.SUSPENDED
      case AgreementState.INACTIVE  => ApiAgreementState.INACTIVE
    }

}
