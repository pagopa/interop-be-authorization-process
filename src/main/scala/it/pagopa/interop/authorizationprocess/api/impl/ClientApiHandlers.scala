package it.pagopa.interop.authorizationprocess.api.impl

import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors._
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.commons.utils.errors.AkkaResponses

import scala.util.{Failure, Success, Try}

object ClientApiHandlers extends AkkaResponses {

  def createConsumerClientResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)  => success(s)
      case Failure(ex) => internalServerError(ex, logMessage)
    }

  def createApiClientResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)  => success(s)
      case Failure(ex) => internalServerError(ex, logMessage)
    }

  def getClientResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                  => success(s)
      case Failure(ex: OrganizationNotAllowedOnClient) => forbidden(ex, logMessage)
      case Failure(ex: ClientNotFound)                 => notFound(ex, logMessage)
      case Failure(ex)                                 => internalServerError(ex, logMessage)
    }

  def getClientsResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                        => success(s)
      case Failure(ex: SecurityOperatorRelationshipNotFound) => forbidden(ex, logMessage)
      case Failure(ex)                                       => internalServerError(ex, logMessage)
    }

  def getClientsWithKeysResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                        => success(s)
      case Failure(ex: SecurityOperatorRelationshipNotFound) => forbidden(ex, logMessage)
      case Failure(ex)                                       => internalServerError(ex, logMessage)
    }

  def deleteClientResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                  => success(s)
      case Failure(ex: OrganizationNotAllowedOnClient) => forbidden(ex, logMessage)
      case Failure(ex: ClientNotFound)                 => notFound(ex, logMessage)
      case Failure(ex)                                 => internalServerError(ex, logMessage)
    }

  def clientOperatorRelationshipBindingResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                         => success(s)
      case Failure(ex: SecurityOperatorRelationshipNotActive) => badRequest(ex, logMessage)
      case Failure(ex: OperatorRelationshipAlreadyAssigned)   => badRequest(ex, logMessage)
      case Failure(ex: OrganizationNotAllowedOnClient)        => forbidden(ex, logMessage)
      case Failure(ex: ClientNotFound)                        => notFound(ex, logMessage)
      case Failure(ex)                                        => internalServerError(ex, logMessage)
    }

  def removeClientOperatorRelationshipResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                         => success(s)
      case Failure(ex: UserNotAllowedToRemoveOwnRelationship) => forbidden(ex, logMessage)
      case Failure(ex: OrganizationNotAllowedOnClient)        => forbidden(ex, logMessage)
      case Failure(ex: ClientRelationshipNotFound)            => notFound(ex, logMessage)
      case Failure(ex)                                        => internalServerError(ex, logMessage)
    }

  def getClientKeyByIdResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                  => success(s)
      case Failure(ex: OrganizationNotAllowedOnClient) => forbidden(ex, logMessage)
      case Failure(ex: ClientNotFound)                 => notFound(ex, logMessage)
      case Failure(ex: ClientKeyNotFound)              => notFound(ex, logMessage)
      case Failure(ex)                                 => internalServerError(ex, logMessage)
    }

  def getClientKeysResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                  => success(s)
      case Failure(ex: OrganizationNotAllowedOnClient) => forbidden(ex, logMessage)
      case Failure(ex: ClientNotFound)                 => notFound(ex, logMessage)
      case Failure(ex)                                 => internalServerError(ex, logMessage)
    }

  def createKeysResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                        => success(s)
      case Failure(ex: CreateKeysBadRequest)                 => badRequest(ex, logMessage)
      case Failure(ex: SecurityOperatorRelationshipNotFound) => forbidden(ex, logMessage)
      case Failure(ex: OrganizationNotAllowedOnClient)       => forbidden(ex, logMessage)
      case Failure(ex: ClientNotFound)                       => notFound(ex, logMessage)
      case Failure(ex: KeysAlreadyExist)                     => conflict(ex, logMessage)
      case Failure(ex)                                       => internalServerError(ex, logMessage)
    }

  def deleteClientKeyByIdResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                  => success(s)
      case Failure(ex: OrganizationNotAllowedOnClient) => forbidden(ex, logMessage)
      case Failure(ex: ClientKeyNotFound)              => notFound(ex, logMessage)
      case Failure(ex)                                 => internalServerError(ex, logMessage)
    }

  def getClientOperatorsResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                  => success(s)
      case Failure(ex: OrganizationNotAllowedOnClient) => forbidden(ex, logMessage)
      case Failure(ex: ClientNotFound)                 => notFound(ex, logMessage)
      case Failure(ex)                                 => internalServerError(ex, logMessage)
    }

  def addClientPurposeResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                   => success(s)
      case Failure(ex: OrganizationNotAllowedOnClient)  => forbidden(ex, logMessage)
      case Failure(ex: OrganizationNotAllowedOnPurpose) => forbidden(ex, logMessage)
      case Failure(ex: AgreementNotFound)               => badRequest(ex, logMessage)
      case Failure(ex: PurposeNoVersionFound)           => badRequest(ex, logMessage)
      case Failure(ex: PurposeNotFound)                 => notFound(ex, logMessage)
      case Failure(ex: ClientNotFound)                  => notFound(ex, logMessage)
      case Failure(ex)                                  => internalServerError(ex, logMessage)
    }

  def removeClientPurposeResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                  => success(s)
      case Failure(ex: OrganizationNotAllowedOnClient) => forbidden(ex, logMessage)
      case Failure(ex: ClientNotFound)                 => notFound(ex, logMessage)
      case Failure(ex)                                 => internalServerError(ex, logMessage)
    }

  def removePurposeFromClientsResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                   => success(s)
      case Failure(ex: PurposeNotFound) => notFound(ex, logMessage)
      case Failure(ex)                  => internalServerError(ex, logMessage)
    }

  def getEncodedClientKeyByIdResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                  => success(s)
      case Failure(ex: OrganizationNotAllowedOnClient) => forbidden(ex, logMessage)
      case Failure(ex: ClientKeyNotFound)              => notFound(ex, logMessage)
      case Failure(ex)                                 => internalServerError(ex, logMessage)
    }
}
