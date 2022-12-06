package it.pagopa.interop.authorizationprocess.error

import akka.http.scaladsl.server.StandardRoute
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors.{
  AgreementNotFound,
  ClientKeyNotFound,
  ClientNotFound,
  ClientRelationshipNotFound,
  OperatorRelationshipAlreadyAssigned,
  OrganizationNotAllowedOnClient,
  PurposeNoVersionFound,
  PurposeNotFound,
  SecurityOperatorRelationshipNotActive,
  SecurityOperatorRelationshipNotFound,
  TenantNotFound,
  UserNotAllowedToRemoveOwnRelationship
}
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.commons.utils.errors.AkkaResponses

import scala.util.{Failure, Try}

object ClientApiHandlers extends AkkaResponses {

  def handleConsumerClientCreationError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = { case Failure(ex) =>
    internalServerError(ex, logMessage)
  }

  def handleApiClientCreationError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = { case Failure(ex) =>
    internalServerError(ex, logMessage)
  }

  def handleClientRetrieveError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = {
    case Failure(ex: OrganizationNotAllowedOnClient) => forbidden(ex, logMessage)
    case Failure(ex: ClientNotFound)                 => notFound(ex, logMessage)
    case Failure(ex)                                 => internalServerError(ex, logMessage)
  }

  def handleClientListingError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = {
    case Failure(ex: TenantNotFound) => notFound(ex, logMessage)
    case Failure(ex)                 => internalServerError(ex, logMessage)
  }

  def handleClientDeleteError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = {
    case Failure(ex: OrganizationNotAllowedOnClient) => forbidden(ex, logMessage)
    case Failure(ex: ClientNotFound)                 => notFound(ex, logMessage)
    case Failure(ex)                                 => internalServerError(ex, logMessage)
  }

  def handleClientOperatorRelationshipBindingError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = {
    case Failure(ex: SecurityOperatorRelationshipNotActive) => badRequest(ex, logMessage)
    case Failure(ex: OperatorRelationshipAlreadyAssigned)   => badRequest(ex, logMessage)
    case Failure(ex: OrganizationNotAllowedOnClient)        => forbidden(ex, logMessage)
    case Failure(ex: ClientNotFound)                        => notFound(ex, logMessage)
    case Failure(ex)                                        => internalServerError(ex, logMessage)
  }

  def handleClientOperatorRelationshipDeleteError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = {
    case Failure(ex: UserNotAllowedToRemoveOwnRelationship) => forbidden(ex, logMessage)
    case Failure(ex: OrganizationNotAllowedOnClient)        => forbidden(ex, logMessage)
    case Failure(ex: ClientRelationshipNotFound)            => notFound(ex, logMessage)
    case Failure(ex)                                        => internalServerError(ex, logMessage)
  }

  def handleClientKeyRetrieveError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = {
    case Failure(ex: OrganizationNotAllowedOnClient) => forbidden(ex, logMessage)
    case Failure(ex: ClientKeyNotFound)              => notFound(ex, logMessage)
    case Failure(ex)                                 => internalServerError(ex, logMessage)
  }

  def handleClientKeysRetrieveError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = {
    case Failure(ex: OrganizationNotAllowedOnClient) => forbidden(ex, logMessage)
    case Failure(ex: ClientNotFound)                 => notFound(ex, logMessage)
    case Failure(ex)                                 => internalServerError(ex, logMessage)
  }

  def handleClientKeyCreationError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = {
    case Failure(ex: SecurityOperatorRelationshipNotFound) => forbidden(ex, logMessage)
    case Failure(ex: OrganizationNotAllowedOnClient)       => forbidden(ex, logMessage)
    case Failure(ex: ClientNotFound)                       => notFound(ex, logMessage)
    case Failure(ex)                                       => internalServerError(ex, logMessage)
  }

  def handleClientKeyDeleteError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = {
    case Failure(ex: OrganizationNotAllowedOnClient) => forbidden(ex, logMessage)
    case Failure(ex: ClientKeyNotFound)              => notFound(ex, logMessage)
    case Failure(ex)                                 => internalServerError(ex, logMessage)
  }

  def handleClientOperatorsRetrieveError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = {
    case Failure(ex: OrganizationNotAllowedOnClient) => forbidden(ex, logMessage)
    case Failure(ex: ClientNotFound)                 => notFound(ex, logMessage)
    case Failure(ex)                                 => internalServerError(ex, logMessage)
  }

  def handleClientOperatorByRelationshipRetrieveError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = {
    case Failure(ex: OrganizationNotAllowedOnClient)       => forbidden(ex, logMessage)
    case Failure(ex: ClientNotFound)                       => notFound(ex, logMessage)
    case Failure(ex: SecurityOperatorRelationshipNotFound) => notFound(ex, logMessage)
    case Failure(ex)                                       => internalServerError(ex, logMessage)
  }

  def handleClientPurposeAdditionError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = {
    case Failure(ex: AgreementNotFound)     => badRequest(ex, logMessage)
    case Failure(ex: PurposeNoVersionFound) => badRequest(ex, logMessage)
    case Failure(ex: PurposeNotFound)       => notFound(ex, logMessage)
    case Failure(ex: ClientNotFound)        => notFound(ex, logMessage)
    case Failure(ex)                        => internalServerError(ex, logMessage)
  }

  def handleClientPurposeRemoveError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = {
    case Failure(ex: OrganizationNotAllowedOnClient) => forbidden(ex, logMessage)
    case Failure(ex: ClientNotFound)                 => notFound(ex, logMessage)
    case Failure(ex)                                 => internalServerError(ex, logMessage)
  }

  def handleEncodedKeyRetrieveError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = {
    case Failure(ex: OrganizationNotAllowedOnClient) => forbidden(ex, logMessage)
    case Failure(ex: ClientKeyNotFound)              => notFound(ex, logMessage)
    case Failure(ex)                                 => internalServerError(ex, logMessage)
  }
}
