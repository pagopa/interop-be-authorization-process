package it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl

import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{CatalogProcessInvoker, CatalogProcessService}
import it.pagopa.pdnd.interopuservice.catalogprocess.client.api.ProcessApi
import it.pagopa.pdnd.interopuservice.catalogprocess.client.invoker.{ApiRequest, BearerToken}
import it.pagopa.pdnd.interopuservice.catalogprocess.client.model.EService
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class CatalogProcessServiceImpl(invoker: CatalogProcessInvoker, api: ProcessApi)(implicit ec: ExecutionContext)
    extends CatalogProcessService {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /** Returns the expected audience defined by the producer of the corresponding agreementId.
    *
    * @param bearerToken
    * @param eServiceId
    * @return
    */
  override def getEService(bearerToken: String, eServiceId: String): Future[EService] = {
    val request: ApiRequest[EService] = api.getEService(eServiceId)(BearerToken(bearerToken))
    invoker
      .execute[EService](request)
      .map { x =>
        logger.info(s"Retrieving E-Service status code > ${x.code.toString}")
        logger.info(s"Retrieving E-Service content > ${x.content.toString}")
        x.content
      }
      .recoverWith { case ex =>
        logger.error(s"Retrieving E-Service, error > ${ex.getMessage}")
        Future.failed[EService](ex)
      }
  }
}
