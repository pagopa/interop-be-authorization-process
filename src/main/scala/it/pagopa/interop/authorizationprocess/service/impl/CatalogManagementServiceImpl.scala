package it.pagopa.interop.authorizationprocess.service.impl

import it.pagopa.interop.authorizationprocess.service.{CatalogManagementInvoker, CatalogManagementService}
import it.pagopa.interop.catalogmanagement.client.api.EServiceApi
import it.pagopa.interop.catalogmanagement.client.invoker.BearerToken
import it.pagopa.interop.catalogmanagement.client.model.EService
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}

import java.util.UUID
import scala.concurrent.Future
import it.pagopa.interop.catalogmanagement.client.invoker.ApiRequest

final case class CatalogManagementServiceImpl(invoker: CatalogManagementInvoker, api: EServiceApi)
    extends CatalogManagementService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getEService(eServiceId: UUID)(implicit contexts: Seq[(String, String)]): Future[EService] =
    withHeaders[EService] { (bearerToken, correlationId, ip) =>
      val request: ApiRequest[EService] =
        api.getEService(xCorrelationId = correlationId, eServiceId.toString, xForwardedFor = ip)(
          BearerToken(bearerToken)
        )
      invoker.invoke(request, "Retrieving EService")
    }
}
