package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.{EService => ApiEService}
import it.pagopa.pdnd.interopuservice.catalogprocess.client.model.EService

import scala.concurrent.Future

trait CatalogProcessService {

  def getEService(bearerToken: String, eServiceId: String): Future[EService]
}

object CatalogProcessService {

  def eServiceToApi(eService: EService): ApiEService =
    ApiEService(eService.id, eService.name)

}
