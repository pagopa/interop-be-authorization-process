package it.pagopa.interop.authorizationprocess.api.impl

import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors.ClientNotFound
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.commons.utils.errors.AkkaResponses

import scala.util.{Failure, Success, Try}
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors.OrganizationNotAllowedOnClient

object OperatorApiHandlers extends AkkaResponses {

  def getClientOperatorKeysResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                  => success(s)
      case Failure(ex: OrganizationNotAllowedOnClient) => forbidden(ex, logMessage)
      case Failure(ex: ClientNotFound)                 => notFound(ex, logMessage)
      case Failure(ex)                                 => internalServerError(ex, logMessage)
    }
}
