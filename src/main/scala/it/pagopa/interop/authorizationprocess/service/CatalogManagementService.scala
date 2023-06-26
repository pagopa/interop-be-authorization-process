package it.pagopa.interop.authorizationprocess.service

import it.pagopa.interop.catalogmanagement.model.{CatalogItem, CatalogDescriptor}
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.authorizationprocess.model.{
  EService => ApiEService,
  EServiceDescriptor => ApiEServiceDescriptor
}

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}

trait CatalogManagementService {
  def getEServiceById(eServiceId: UUID)(implicit ec: ExecutionContext, readModel: ReadModelService): Future[CatalogItem]
}

object CatalogManagementService {
  def eServiceToApi(eService: CatalogItem): ApiEService = ApiEService(id = eService.id, name = eService.name)
  def descriptorToApi(descriptor: CatalogDescriptor): ApiEServiceDescriptor =
    ApiEServiceDescriptor(id = descriptor.id, version = descriptor.version)
}
