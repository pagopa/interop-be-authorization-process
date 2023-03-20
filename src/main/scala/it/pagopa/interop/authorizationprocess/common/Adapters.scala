package it.pagopa.interop.authorizationprocess.common

import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationmanagement.model.client._
import it.pagopa.interop.authorizationmanagement.model.client.PersistentClientComponentState.Active
import it.pagopa.interop.authorizationmanagement.model.client.PersistentClientComponentState.Inactive
import it.pagopa.interop.authorizationmanagement.model.client.{Api, Consumer}
import it.pagopa.interop.authorizationmanagement.client.{model => AuthorizationManagementDependency}

object Adapters {

  implicit class PersistentClientWrapper(private val p: PersistentClient) extends AnyVal {
    def toApi(showRelationShips: Boolean): ClientEntry = ClientEntry(
      id = p.id,
      name = p.name,
      description = p.description,
      consumerId = p.consumerId,
      purposes = p.purposes.map(_.toApi),
      relationshipsIds = if (showRelationShips) p.relationships else Set.empty,
      kind = p.kind.toApi
    )
  }

  implicit class ManagementClientWrapper(private val p: AuthorizationManagementDependency.Client) extends AnyVal {
    def toApi(showRelationShips: Boolean): ClientEntry = ClientEntry(
      id = p.id,
      name = p.name,
      description = p.description,
      consumerId = p.consumerId,
      purposes = p.purposes.map(_.states.toApi),
      relationshipsIds = if (showRelationShips) p.relationships else Set.empty,
      kind = p.kind.toApi
    )
  }

  implicit class PersistentClientKindWrapper(private val pck: PersistentClientKind) extends AnyVal {
    def toApi: ClientKind = pck match {
      case Api      => ClientKind.API
      case Consumer => ClientKind.CONSUMER
    }
  }

  implicit class ManagementClientKindWrapper(private val mck: AuthorizationManagementDependency.ClientKind)
      extends AnyVal {
    def toApi: ClientKind = mck match {
      case AuthorizationManagementDependency.ClientKind.API      => ClientKind.API
      case AuthorizationManagementDependency.ClientKind.CONSUMER => ClientKind.CONSUMER
    }
  }

  implicit class ClientKindWrapper(private val ck: ClientKind) extends AnyVal {
    def toProcess: PersistentClientKind = ck match {
      case ClientKind.API      => Api
      case ClientKind.CONSUMER => Consumer
    }
  }

  implicit class ClientComponentStateWrapper(private val ck: PersistentClientComponentState) extends AnyVal {
    def toApi: ClientComponentState = ck match {
      case Active   => ClientComponentState.ACTIVE
      case Inactive => ClientComponentState.INACTIVE
    }
  }

  implicit class ClientStatesChainWrapper(private val csc: PersistentClientStatesChain) extends AnyVal {
    def toApi: ClientStatesChain = ClientStatesChain(
      id = csc.id,
      eservice = csc.eService.toApi,
      agreement = csc.agreement.toApi,
      purpose = csc.purpose.toApi
    )
  }

  implicit class ClientEServiceDetailsWrapper(private val ced: PersistentClientEServiceDetails) extends AnyVal {
    def toApi: ClientEServiceDetails = ClientEServiceDetails(
      eserviceId = ced.eServiceId,
      audience = ced.audience,
      voucherLifespan = ced.voucherLifespan,
      state = ced.state.toApi
    )
  }

  implicit class ClientAgreementDetailsWrapper(private val cad: PersistentClientAgreementDetails) extends AnyVal {
    def toApi: ClientAgreementDetails =
      ClientAgreementDetails(eserviceId = cad.eServiceId, consumerId = cad.consumerId, state = cad.state.toApi)
  }

  implicit class ClientPurposeDetailsWrapper(private val cad: PersistentClientPurposeDetails) extends AnyVal {
    def toApi: ClientPurposeDetails = ClientPurposeDetails(purposeId = cad.purposeId, state = cad.state.toApi)
  }

  implicit class ManagementPurposeDetailsWrapper(
    private val cad: AuthorizationManagementDependency.ClientPurposeDetails
  ) extends AnyVal {
    def toApi: ClientPurposeDetails = ClientPurposeDetails(purposeId = cad.purposeId, state = cad.state.toApi)
  }

  implicit class ManagementClientStatesChainWrapper(
    private val csc: AuthorizationManagementDependency.ClientStatesChain
  ) extends AnyVal {
    def toApi: ClientStatesChain = ClientStatesChain(
      id = csc.id,
      eservice = csc.eservice.toApi,
      agreement = csc.agreement.toApi,
      purpose = csc.purpose.toApi
    )
  }

  implicit class ManagementClientEServiceDetailsWrapper(
    private val ced: AuthorizationManagementDependency.ClientEServiceDetails
  ) extends AnyVal {
    def toApi: ClientEServiceDetails = ClientEServiceDetails(
      eserviceId = ced.eserviceId,
      audience = ced.audience,
      voucherLifespan = ced.voucherLifespan,
      state = ced.state.toApi
    )
  }

  implicit class ManagementClientAgreementDetailsWrapper(
    private val cad: AuthorizationManagementDependency.ClientAgreementDetails
  ) extends AnyVal {
    def toApi: ClientAgreementDetails =
      ClientAgreementDetails(eserviceId = cad.eserviceId, consumerId = cad.consumerId, state = cad.state.toApi)
  }

  implicit class ManagementClientComponentStateWrapper(
    private val ck: AuthorizationManagementDependency.ClientComponentState
  ) extends AnyVal {
    def toApi: ClientComponentState = ck match {
      case AuthorizationManagementDependency.ClientComponentState.ACTIVE   => ClientComponentState.ACTIVE
      case AuthorizationManagementDependency.ClientComponentState.INACTIVE => ClientComponentState.INACTIVE
    }
  }
}
