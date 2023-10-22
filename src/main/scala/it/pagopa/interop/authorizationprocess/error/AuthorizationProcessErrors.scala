package it.pagopa.interop.authorizationprocess.error

import it.pagopa.interop.commons.utils.errors.ComponentError

import java.util.UUID

object AuthorizationProcessErrors {

  final case class UserAlreadyAssigned(clientId: UUID, userId: UUID)
      extends ComponentError(
        "0001",
        s"Operator user ${userId.toString} is already assigned to the client ${clientId.toString}"
      )

  final case class SecurityUserNotFound(consumerId: UUID, userId: UUID)
      extends ComponentError(
        "0003",
        s"Security operator user not found for consumer ${consumerId.toString} and user ${userId.toString}"
      )

  final case class UserNotAllowedToRemoveOwnUser(clientId: String, userId: String)
      extends ComponentError(
        "0004",
        s"A user is not allowed to remove own user from client. Client $clientId, User $userId"
      )

  final case class AgreementNotFound(eServiceId: UUID, consumerId: UUID)
      extends ComponentError("0005", s"Agreement not found for EService $eServiceId and Consumer $consumerId")

  final case class DescriptorNotFound(eServiceId: UUID, descriptorId: UUID)
      extends ComponentError("0006", s"Descriptor $descriptorId not found for EService $eServiceId")

  final case class MissingUserInfo(userId: UUID) extends ComponentError("0007", s"Missing ${userId.toString} user info")

  final case class OrganizationNotAllowedOnClient(clientId: String, organizationId: UUID)
      extends ComponentError("0008", s"Organization $organizationId is not allowed on client $clientId")

  final case object MissingSelfcareId extends ComponentError("0009", "SelfcareId in tenant not found")

  final case class ClientNotFound(clientId: UUID) extends ComponentError("0010", s"Client $clientId not found")

  final case class TenantNotFound(tenantId: UUID) extends ComponentError("0011", s"Tenant $tenantId not found")

  final case class ClientUserNotFound(clientId: UUID, userId: UUID)
      extends ComponentError("0012", s"User $userId not found for Client $clientId")

  final case class ClientKeyNotFound(clientId: UUID, kid: String)
      extends ComponentError("0013", s"Key $kid not found for Client $clientId")

  final case class PurposeNotFound(purposeId: UUID) extends ComponentError("0014", s"Purpose $purposeId not found")

  final case class PurposeNoVersionFound(purposeId: UUID)
      extends ComponentError("0015", s"No version found in Purpose $purposeId")

  final case class OrganizationNotAllowedOnPurpose(purposeId: String, organizationId: String)
      extends ComponentError("0016", s"Organization $organizationId is not allowed on purpose $purposeId")

  final case class CreateKeysBadRequest(message: String)
      extends ComponentError("0017", s"Unable to create keys: $message")

  final case class KeysAlreadyExist(message: String)
      extends ComponentError("0018", s"One or more keys already exist: $message")

  final case class EServiceNotFound(eServiceId: UUID)
      extends ComponentError("0019", s"EService ${eServiceId.toString} not found")

  final case class MissingUserId(kid: String) extends ComponentError("0020", s"Key $kid has not UserId")

  final case class SelfcareEntityNotFilled(className: String, field: String)
      extends ComponentError("0021", s"Selfcare entity $className with field $field not filled")

  final case class InstitutionNotFound(selfcareId: UUID)
      extends ComponentError("0022", s"Institution $selfcareId not found")

  final case class UserNotFound(selfcareId: UUID, userId: UUID)
      extends ComponentError("0023", s"User $userId not found for institution $selfcareId")
}
