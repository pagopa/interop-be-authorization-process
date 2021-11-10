package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

import java.util.UUID

final case class SecurityOperatorRelationshipNotFound(consumerId: UUID, relationshipId: UUID)
    extends Throwable(
      s"Security operator relationship not found for consumer ${consumerId.toString} and relationship ${relationshipId.toString}"
    )
