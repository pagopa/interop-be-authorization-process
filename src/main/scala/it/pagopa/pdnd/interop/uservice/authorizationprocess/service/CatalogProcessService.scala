package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import it.pagopa.pdnd.interopuservice.catalogprocess.client.model.EService

import scala.concurrent.Future

trait CatalogProcessService {

  /** Returns the expected audience defined by the producer of the corresponding agreementId.
    *
    * @param agreementId
    * @return
    */

  def getEService(bearerToken: String, eServiceId: String): Future[EService]
}
