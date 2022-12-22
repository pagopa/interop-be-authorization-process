package it.pagopa.interop.authorizationprocess.service.impl

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors.PurposeNotFound
import it.pagopa.interop.authorizationprocess.service.{PurposeManagementInvoker, PurposeManagementService}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.withHeaders
import it.pagopa.interop.purposemanagement.client.api.PurposeApi
import it.pagopa.interop.purposemanagement.client.invoker.{ApiError, ApiRequest, BearerToken}
import it.pagopa.interop.purposemanagement.client.model.Purpose

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

final case class PurposeManagementServiceImpl(invoker: PurposeManagementInvoker, api: PurposeApi)(implicit
  ec: ExecutionContext
) extends PurposeManagementService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getPurpose(purposeId: UUID)(implicit contexts: Seq[(String, String)]): Future[Purpose] =
    withHeaders[Purpose] { (bearerToken, correlationId, ip) =>
      val request: ApiRequest[Purpose] =
        api.getPurpose(xCorrelationId = correlationId, purposeId, xForwardedFor = ip)(BearerToken(bearerToken))
      invoker
        .invoke(request, "Retrieving Purpose")
        .recoverWith {
          case err: ApiError[_] if err.code == 404 => Future.failed(PurposeNotFound(purposeId))
        }
    }
}
