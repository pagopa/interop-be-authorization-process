package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.{
  EService => ApiEService,
  Organization => ApiOrganization,
  Descriptor => ApiDescriptor
}
import it.pagopa.pdnd.interop.uservice.catalogmanagement.client.model.{
  EService,
  EServiceDescriptor,
  EServiceDescriptorEnums
}

import scala.concurrent.Future

trait CatalogManagementService {

  def getEService(bearerToken: String, eServiceId: String): Future[EService]
}

object CatalogManagementService {

  def eServiceToApi(
    eService: EService,
    provider: ApiOrganization,
    activeDescriptor: Option[EServiceDescriptor]
  ): ApiEService =
    ApiEService(eService.id, eService.name, provider, activeDescriptor.map(descriptorToApi))

  def descriptorToApi(descriptor: EServiceDescriptor): ApiDescriptor =
    ApiDescriptor(descriptor.id, descriptor.status.toString, descriptor.version)

  def getActiveDescriptor(eService: EService): Option[EServiceDescriptor] =
    eService.descriptors.find(_.status == EServiceDescriptorEnums.Status.Published)

}
