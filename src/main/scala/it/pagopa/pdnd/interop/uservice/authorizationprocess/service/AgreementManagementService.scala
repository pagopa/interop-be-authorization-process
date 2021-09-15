package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.model.Agreement
import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.model.AgreementEnums.Status
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.{
  Agreement => ApiAgreement,
  Descriptor => ApiDescriptor
}

import scala.concurrent.Future

trait AgreementManagementService {

  /** Returns the expected audience defined by the producer of the corresponding agreementId.
    *
    * @param agreementId
    * @return
    */

  def getAgreements(
    bearerToken: String,
    consumerId: String,
    eserviceId: String,
    status: Option[Status]
  ): Future[Seq[Agreement]]
}

object AgreementManagementService {
  def agreementToApi(agreement: Agreement, descriptor: ApiDescriptor): ApiAgreement =
    ApiAgreement(id = agreement.id, status = agreement.status.toString, descriptor = descriptor)
}
