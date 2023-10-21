package it.pagopa.interop.authorizationprocess.common.model

import java.util.UUID

final case class UserResponse(email: String, id: UUID, name: String, surname: String, taxCode: String)
