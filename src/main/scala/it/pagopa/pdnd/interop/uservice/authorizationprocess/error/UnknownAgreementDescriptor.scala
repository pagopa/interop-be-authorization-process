package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

final case class UnknownAgreementDescriptor(agreementId: String, eServiceId: String, descriptorId: String)
    extends Throwable(s"Unable to find descriptor $descriptorId in E-Service $eServiceId for agreement $agreementId")
