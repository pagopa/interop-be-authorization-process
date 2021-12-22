package it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl

import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{CatalogManagementInvoker, CatalogManagementService}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.client.api.EServiceApi
import it.pagopa.pdnd.interop.uservice.catalogmanagement.client.invoker.{ApiRequest, BearerToken}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.client.model.EService
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class CatalogManagementServiceImpl(invoker: CatalogManagementInvoker, api: EServiceApi)(implicit ec: ExecutionContext)
    extends CatalogManagementService {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /** Returns the expected audience defined by the producer of the corresponding agreementId.
    *
    * @param bearerToken
    * @param eServiceId
    * @return
    */
  override def getEService(bearerToken: String, eServiceId: UUID): Future[EService] = {
    val request: ApiRequest[EService] = api.getEService(eServiceId.toString)(BearerToken(bearerToken))
    invoker
      .execute[EService](request)
      .map { x =>
        logger.info(s"Retrieving E-Service status code > ${x.code.toString}")
        logger.info(s"Retrieving E-Service content > ${x.content.toString}")
        x.content
      }
      .recoverWith { case ex =>
        logger.error(s"Retrieving E-Service FAILED", ex)
        Future.failed[EService](ex)
      }
  }
}
