package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

import java.util.UUID

final case class SecurityOperatorRelationshipNotFound(consumerId: UUID, operatorId: UUID)
    extends Throwable(
      s"Security operator relationship not found for consumer ${consumerId.toString} and operator ${operatorId.toString}"
    )
