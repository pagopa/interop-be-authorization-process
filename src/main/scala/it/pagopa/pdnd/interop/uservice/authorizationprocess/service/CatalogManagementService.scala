package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.{EService => ApiEService}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.client.model.EService

import scala.concurrent.Future

trait CatalogManagementService {

  def getEService(bearerToken: String, eServiceId: String): Future[EService]
}

object CatalogManagementService {

  def eServiceToApi(eService: EService): ApiEService =
    ApiEService(eService.id, eService.name)

}
