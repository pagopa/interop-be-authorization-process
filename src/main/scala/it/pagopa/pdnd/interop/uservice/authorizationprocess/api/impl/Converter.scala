package it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl

import it.pagopa.interop.authorizationmanagement.client.model.{ClientKind => ManagementClientKind}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.ClientKind

object Converter {

  def convertToApiClientKind(kind: ManagementClientKind): ClientKind =
    kind match {
      case ManagementClientKind.CONSUMER => ClientKind.CONSUMER
      case ManagementClientKind.API      => ClientKind.API
    }

  def convertFromApiClientKind(kind: ClientKind): ManagementClientKind =
    kind match {
      case ClientKind.CONSUMER => ManagementClientKind.CONSUMER
      case ClientKind.API      => ManagementClientKind.API
    }
}
