package it.pagopa.interop.authorizationprocess.service.impl

import it.pagopa.interop.authorizationprocess.service.{CatalogManagementInvoker, CatalogManagementService}
import it.pagopa.interop.catalogmanagement.client.api.EServiceApi
import it.pagopa.interop.catalogmanagement.client.invoker.{ApiRequest, BearerToken}
import it.pagopa.interop.catalogmanagement.client.model.EService
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.Future

final case class CatalogManagementServiceImpl(invoker: CatalogManagementInvoker, api: EServiceApi)
    extends CatalogManagementService {

  implicit val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def getEService(bearerToken: String)(eServiceId: UUID): Future[EService] = {
    val request: ApiRequest[EService] = api.getEService(eServiceId.toString)(BearerToken(bearerToken))
    invoker.invoke(request, "Retrieving EService")
  }
}
