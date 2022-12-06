package it.pagopa.interop.authorizationprocess.error

import akka.http.scaladsl.server.StandardRoute
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors.ClientNotFound
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.commons.utils.errors.AkkaResponses

import scala.util.{Failure, Try}

object OperatorApiHandlers extends AkkaResponses {

  def handleClientOperatorKeysRetrieveError(logMessage: String)(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): PartialFunction[Try[_], StandardRoute] = {
    case Failure(ex: ClientNotFound) => notFound(ex, logMessage)
    case Failure(ex)                 => internalServerError(ex, logMessage)
  }
}
