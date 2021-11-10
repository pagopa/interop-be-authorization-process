package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

import java.util.UUID

final case class SecurityOperatorRelationshipNotActive(relationshipId: UUID)
    extends Throwable(s"Relationship ${relationshipId} is not an active relationship for a security Operator.")
