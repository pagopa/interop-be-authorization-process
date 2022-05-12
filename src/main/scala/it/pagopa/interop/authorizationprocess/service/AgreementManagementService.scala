package it.pagopa.interop.authorizationprocess.service

import it.pagopa.interop.agreementmanagement.client.model.Agreement
import it.pagopa.interop.authorizationprocess.model.{
  Agreement => ApiAgreement,
  EService => ApiEService,
  EServiceDescriptor => ApiEServiceDescriptor
}

import java.util.UUID
import scala.concurrent.Future

trait AgreementManagementService {
  def getAgreements(eServiceId: UUID, consumerId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[Seq[Agreement]]

}

object AgreementManagementService {
  def agreementToApi(agreement: Agreement, eService: ApiEService, descriptor: ApiEServiceDescriptor): ApiAgreement =
    ApiAgreement(id = agreement.id, eservice = eService, descriptor = descriptor)

}
