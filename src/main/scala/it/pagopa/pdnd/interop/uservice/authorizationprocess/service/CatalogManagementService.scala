package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import it.pagopa.pdnd.interop.uservice.catalogmanagement.client.model.EService

import scala.concurrent.Future

trait CatalogManagementService {

  /** Returns the expected audience defined by the producer of the corresponding eserviceId.
    *
    * @param eserviceId
    * @return
    */

  def getEService(bearerToken: String, eServiceId: String): Future[EService]
}
