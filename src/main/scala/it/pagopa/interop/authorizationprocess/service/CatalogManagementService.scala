package it.pagopa.interop.authorizationprocess.service

import it.pagopa.interop.catalogmanagement.client.model.EService

import java.util.UUID
import scala.concurrent.Future

trait CatalogManagementService {
  def getEService(bearerToken: String)(eServiceId: UUID): Future[EService]

}
