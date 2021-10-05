package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

final case class EServiceDescriptorNotActive(clientId: String) extends Throwable(s"Client $clientId is not active")
