package it.pagopa.interop.authorizationprocess.error

import it.pagopa.interop.commons.utils.errors.ComponentError

import java.util.UUID

object AuthorizationProcessErrors {

  final case class EnumParameterError(fieldName: String, values: Seq[String])
      extends ComponentError(
        "0005",
        s"Invalid parameter value. parameter '$fieldName' should be in ${values.mkString("[", ",", "]")}"
      )

  final case class InvalidAccessTokenRequest(errors: List[String])
      extends ComponentError("0007", s"Invalid access token request: ${errors.mkString(", ")}")

  final case class OperatorRelationshipAlreadyAssigned(clientId: UUID, operatorRelationship: UUID)
      extends ComponentError(
        "0009",
        s"Operator relationship ${operatorRelationship.toString} is already assigned to the client ${clientId.toString}"
      )

  final case class SecurityOperatorRelationshipNotActive(relationshipId: UUID)
      extends ComponentError(
        "0010",
        s"Relationship $relationshipId is not an active relationship for a security Operator."
      )

  final case class SecurityOperatorRelationshipNotFound(consumerId: UUID, relationshipId: UUID)
      extends ComponentError(
        "0011",
        s"Security operator relationship not found for consumer ${consumerId.toString} and relationship ${relationshipId.toString}"
      )

  final case class UUIDConversionError(value: String)
      extends ComponentError("0014", s"Unable to convert $value to uuid")

  final case object NoResultsError extends ComponentError("0018", "No operation executed")

  final case object OperatorKeyCreationError      extends ComponentError("0021", "Operator key creation error")
  final case object OperatorKeyDeletionError      extends ComponentError("0022", "Error on operator key delete")
  final case object OperatorKeyRetrievalError     extends ComponentError("0023", "Error on key retrieve")
  final case object OperatorKeysRetrievalError    extends ComponentError("0024", "Error on operator keys retrieve")
  final case object ClientKeysRetrievalError      extends ComponentError("0025", "Error on client keys retrieve")
  final case object ClientCreationError           extends ComponentError("0026", "Error on client creation")
  final case object ClientRetrievalError          extends ComponentError("0027", "Error on client retrieval")
  final case object ClientListingError            extends ComponentError("0028", "Error on client listing")
  final case object ClientDeletionError           extends ComponentError("0029", "Error on client deletion")
  final case object OperatorAdditionError         extends ComponentError("0030", "Error on operator addition")
  final case object OperatorRemovalError          extends ComponentError("0031", "Error on operator removal")
  final case object ClientKeyRetrievalError       extends ComponentError("0032", "Error on client key retrieve")
  final case object ClientKeyDeletionError        extends ComponentError("0033", "Error on client key delete")
  final case object ClientKeyCreationError        extends ComponentError("0034", "Error on client key creation")
  final case object ClientOperatorsRetrievalError extends ComponentError("0035", "Error on client operators retrieval")
  final case object ClientOperatorsRelationshipRetrievalError
      extends ComponentError("0036", "Error on client operators relationships retrieval")

  final case object EncodedClientKeyRetrievalError
      extends ComponentError("0041", "Error on encoded client key retrieval")

  final case object WellKnownRetrievalError
      extends ComponentError("0042", "Something goes wrong trying to get well-known keys")

  final case class ClientPurposeAddError(clientId: String, purposeId: String)
      extends ComponentError("0043", s"Error adding Purpose $purposeId to Client $clientId")

  final case class ClientPurposeRemoveError(clientId: String, purposeId: String)
      extends ComponentError("0044", s"Error removing Purpose $purposeId from Client $clientId")

  final case class ClientPurposeAddAgreementNotFound(eServiceId: String, consumerId: String)
      extends ComponentError(
        "0045",
        s"Not acceptable agreement found for EService $eServiceId and Consumer $consumerId"
      )

  final case class ClientPurposeAddDescriptorNotFound(eServiceId: String, descriptorId: String)
      extends ComponentError("0046", s"Descriptor $descriptorId not found in EService $eServiceId")

  final case class UserNotAllowedToRemoveOwnRelationship(clientId: String, relationshipId: String)
      extends ComponentError(
        "0047",
        s"A user is not allowed to remove own relationship from client. Client $clientId Relationship $relationshipId"
      )

  final case class AgreementNotFound(eServiceId: String, consumerId: String)
      extends ComponentError("0048", s"Agreement not found for EService $eServiceId and Consumer $consumerId")

  final case class DescriptorNotFound(eServiceId: String, descriptorId: String)
      extends ComponentError("0049", s"Descriptor $descriptorId not found for EService $eServiceId")

  final case class MissingUserInfo(userId: UUID) extends ComponentError("0050", s"Missing ${userId.toString} user info")

}
