package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

import java.util.UUID

final case class ClientNotActive(clientId: UUID) extends Throwable(s"Client ${clientId.toString} is not active")
