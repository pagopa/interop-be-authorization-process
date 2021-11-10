package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

import java.util.UUID

final case class OperatorRelationshipAlreadyAssigned(clientId: UUID, operatorRelationship: UUID)
    extends Throwable(
      s"Operator relationship ${operatorRelationship.toString} is already assigned to the client ${clientId.toString}"
    )
