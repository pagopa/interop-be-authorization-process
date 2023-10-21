package it.pagopa.interop.authorizationprocess.common.model

import java.util.UUID

final case class UserResource(
  email: String,
  id: UUID,
  name: String,
  surname: String,
  fiscalCode: String,
  roles: Seq[String]
)
