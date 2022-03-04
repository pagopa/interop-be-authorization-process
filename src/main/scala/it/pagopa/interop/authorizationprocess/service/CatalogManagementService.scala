package it.pagopa.interop.authorizationprocess.service

import it.pagopa.interop.catalogmanagement.client.model.{EService, EServiceDescriptor, EServiceDescriptorState}
import it.pagopa.interop.authorizationprocess.model.{
  EServiceDescriptorState => ApiEServiceDescriptorState,
  EService => ApiEService,
  EServiceDescriptor => ApiEServiceDescriptor
}

import java.util.UUID
import scala.concurrent.Future

trait CatalogManagementService {
  def getEService(bearerToken: String)(eServiceId: UUID): Future[EService]

}

object CatalogManagementService {
  def eServiceToApi(eService: EService): ApiEService = ApiEService(id = eService.id, name = eService.name)
  def descriptorToApi(descriptor: EServiceDescriptor): ApiEServiceDescriptor =
    ApiEServiceDescriptor(id = descriptor.id, version = descriptor.version, state = stateToApi(descriptor.state))

  def stateToApi(state: EServiceDescriptorState): ApiEServiceDescriptorState =
    state match {
      case EServiceDescriptorState.DRAFT      => ApiEServiceDescriptorState.DRAFT
      case EServiceDescriptorState.PUBLISHED  => ApiEServiceDescriptorState.PUBLISHED
      case EServiceDescriptorState.DEPRECATED => ApiEServiceDescriptorState.DEPRECATED
      case EServiceDescriptorState.SUSPENDED  => ApiEServiceDescriptorState.SUSPENDED
      case EServiceDescriptorState.ARCHIVED   => ApiEServiceDescriptorState.ARCHIVED
    }
}
