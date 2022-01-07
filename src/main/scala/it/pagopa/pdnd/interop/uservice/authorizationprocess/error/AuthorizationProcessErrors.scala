package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

import it.pagopa.pdnd.interop.commons.utils.errors.ComponentError

import java.util.UUID

object AuthorizationProcessErrors {

  final case class AgreementNotFoundError(eserviceId: UUID, consumerId: UUID)
      extends ComponentError(
        "0001",
        s"No active agreement was found for eservice/consumer. ${eserviceId.toString}/${consumerId.toString}"
      )

  final case class ClientAgreementNotFoundError(clientId: UUID)
      extends ComponentError("0002", s"No agreement found for client ${clientId.toString}")

  final case class ClientNotActive(clientId: UUID)
      extends ComponentError("0003", s"Client ${clientId.toString} is not active")

  /** ADT modeling a not found descriptor error
    *
    * @param eserviceId - identifier of the eservice to lookup
    * @param descriptorId - identifier of the descriptor to lookup
    */
  final case class DescriptorNotFound(eserviceId: UUID, descriptorId: UUID)
      extends ComponentError(
        "0004",
        s"Descriptor ${descriptorId.toString} not found in eservice ${eserviceId.toString}"
      )

  final case class EnumParameterError(fieldName: String, values: Seq[String])
      extends ComponentError(
        "0005",
        s"Invalid parameter value. parameter '$fieldName' should be in ${values.mkString("[", ",", "]")}"
      )

  final case class EServiceDescriptorNotActive(clientId: UUID)
      extends ComponentError("0006", s"Client ${clientId.toString} is not active")

  final case class InvalidAccessTokenRequest(errors: List[String])
      extends ComponentError("0007", s"Invalid access token request: ${errors.mkString(", ")}")

  final case class KeyNotActiveError(kid: String) extends ComponentError("0008", s"Key with kid $kid is not active")

  final case class OperatorRelationshipAlreadyAssigned(clientId: UUID, operatorRelationship: UUID)
      extends ComponentError(
        "0009",
        s"Operator relationship ${operatorRelationship.toString} is already assigned to the client ${clientId.toString}"
      )

  final case class SecurityOperatorRelationshipNotActive(relationshipId: UUID)
      extends ComponentError(
        "0010",
        s"Relationship ${relationshipId} is not an active relationship for a security Operator."
      )

  final case class SecurityOperatorRelationshipNotFound(consumerId: UUID, relationshipId: UUID)
      extends ComponentError(
        "0011",
        s"Security operator relationship not found for consumer ${consumerId.toString} and relationship ${relationshipId.toString}"
      )

  final case class TooManyActiveAgreementsError(eserviceId: UUID, consumerId: UUID)
      extends ComponentError(
        "0012",
        s"Too many active agreements were found for eservice(${eserviceId.toString})/consumer(${consumerId.toString})"
      )

  final case class UnknownAgreementDescriptor(agreementId: UUID, eServiceId: UUID, descriptorId: UUID)
      extends ComponentError(
        "0013",
        s"Unable to find descriptor ${descriptorId.toString} in E-Service ${eServiceId.toString} for agreement ${agreementId.toString}"
      )

  final case class UUIDConversionError(value: String)
      extends ComponentError("0014", s"Unable to convert $value to uuid")

  final case object InvalidClientAssertionType extends ComponentError("0015", s"[Invalid client credential type]")

  final case object InvalidGrantType extends ComponentError("0016", s"[Invalid grant type]")

  final object InvalidJWTSign                     extends ComponentError("0017", "Invalid JWT sign")
  final case class InvalidJWTError(error: String) extends ComponentError("0019", s"Invalid JWT - error: $error")
  final object JWTBadRequest                      extends ComponentError("0020", "Invalid JWT token - bad request")

  final case object NoResultsError extends ComponentError("0018", "No operation executed")

  final case object OperatorKeyCreationError      extends ComponentError("0019", "Operator key creation error")
  final case object OperatorKeyDeletionError      extends ComponentError("0020", "Error on operator key delete")
  final case object OperatorKeyRetrievalError     extends ComponentError("0021", "Error on key retrieve")
  final case object OperatorKeysRetrievalError    extends ComponentError("0022", "Error on operator keys retrieve")
  final case object ClientKeysRetrievalError      extends ComponentError("0023", "Error on client keys retrieve")
  final case object ClientCreationError           extends ComponentError("0024", "Error on client creation")
  final case object ClientRetrievalError          extends ComponentError("0025", "Error on client retrieval")
  final case object ClientListingError            extends ComponentError("0025", "Error on client listing")
  final case object ClientDeletionError           extends ComponentError("0026", "Error on client deletion")
  final case object OperatorAdditionError         extends ComponentError("0027", "Error on operator addition")
  final case object OperatorRemovalError          extends ComponentError("0028", "Error on operator removal")
  final case object ClientKeyRetrievalError       extends ComponentError("0029", "Error on client key retrieve")
  final case object ClientKeyDeletionError        extends ComponentError("0030", "Error on client key delete")
  final case object ClientKeyCreationError        extends ComponentError("0031", "Error on client key creation")
  final case object ClientOperatorsRetrievalError extends ComponentError("0032", "Error on client operators retrieval")
  final case object ClientOperatorsRelationshipRetrievalError
      extends ComponentError("0033", "Error on client operators relationships retrieval")

  final case object ClientActivationBadRequest extends ComponentError("0034", "Client activation bad request")
  final case object ClientActivationError      extends ComponentError("0035", "Error on client activation")

  final case object ClientSuspensionBadRequest extends ComponentError("0036", "Client suspension bad request")
  final case object ClientSuspensionError      extends ComponentError("0037", "Error on client suspension")
  final case object EncodedClientKeyRetrievalError
      extends ComponentError("0038", "Error on encoded client key retrieval")

  final case object WellKnownRetrievalError
      extends ComponentError("0029", "Something goes wrong trying to get well-known keys")

}
