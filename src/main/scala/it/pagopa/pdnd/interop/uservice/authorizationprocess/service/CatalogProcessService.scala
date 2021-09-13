package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import it.pagopa.pdnd.interopuservice.catalogprocess.client.model.EService

import scala.concurrent.Future

trait CatalogProcessService {

  def getEService(bearerToken: String, eServiceId: String): Future[EService]
}
