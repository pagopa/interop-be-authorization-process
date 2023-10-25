package it.pagopa.interop.authorizationprocess.service.model

import java.util.UUID

final case class UserResource(id: UUID, name: String, surname: String, fiscalCode: String, roles: Seq[String])
