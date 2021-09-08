package it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl

import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.model.{Agreement, AgreementEnums}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.error.AgreementStatusNotValidError

import scala.concurrent.Future

trait Validation {
  import Validation._

  def validateUsableAgreementStatus(agreement: Agreement): Future[AgreementEnums.Status] =
    if (validAgreementStatuses.contains(agreement.status)) Future.successful(agreement.status)
    else Future.failed(AgreementStatusNotValidError(agreement.id.toString))
}

object Validation {
  val validAgreementStatuses = Set(AgreementEnums.Status.Active, AgreementEnums.Status.Pending)
}
