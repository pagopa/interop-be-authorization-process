package it.pagopa.interop.authorizationprocess.service

import it.pagopa.interop.authorizationprocess.model.{
  EService => ApiEService,
  EServiceDescriptor => ApiEServiceDescriptor
}
import it.pagopa.interop.catalogmanagement.client.model.{EService, EServiceDescriptor}

import java.util.UUID
import scala.concurrent.Future

trait CatalogManagementService {
  def getEService(bearerToken: String)(eServiceId: UUID): Future[EService]

}

object CatalogManagementService {
  def eServiceToApi(eService: EService): ApiEService = ApiEService(id = eService.id, name = eService.name)
  def descriptorToApi(descriptor: EServiceDescriptor): ApiEServiceDescriptor =
    ApiEServiceDescriptor(id = descriptor.id, version = descriptor.version)
}
