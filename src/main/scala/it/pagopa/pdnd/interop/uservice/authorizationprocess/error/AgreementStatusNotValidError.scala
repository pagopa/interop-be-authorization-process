package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.Validation

final case class AgreementStatusNotValidError(agreementId: String)
    extends Throwable(
      s"Operation not allowed on agreement $agreementId. Status must be ${Validation.validAgreementStatuses
        .mkString("[", ",", "]")}"
    )
