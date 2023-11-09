package it.pagopa.interop.authorizationprocess.common

import it.pagopa.interop.authorizationmanagement.client.model.KeyUse
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationprocess.model.{KeyUse => ProcessKeyUse}
import it.pagopa.interop.authorizationmanagement.model.client._
import it.pagopa.interop.authorizationmanagement.model.client.PersistentClientComponentState.Active
import it.pagopa.interop.authorizationmanagement.model.client.PersistentClientComponentState.Inactive
import it.pagopa.interop.authorizationmanagement.model.client.{Api, Consumer}
import it.pagopa.interop.authorizationmanagement.model.key.{Enc, Sig}
import it.pagopa.interop.authorizationmanagement.client.{model => AuthorizationManagementDependency}
import it.pagopa.interop.authorizationprocess.common.readmodel.model.ReadModelClientWithKeys
import it.pagopa.interop.authorizationmanagement.model.key.{PersistentKey, PersistentKeyUse}
import it.pagopa.interop.authorizationmanagement.jwk.model.Models._

import java.util.UUID
import java.time.OffsetDateTime

object Adapters {

  implicit class PersistentClientWrapper(private val p: PersistentClient) extends AnyVal {
    def toApi(showRelationShips: Boolean): Client = Client(
      id = p.id,
      name = p.name,
      description = p.description,
      consumerId = p.consumerId,
      purposes = p.purposes.map(p => ClientPurpose(states = p.toApi)),
      relationshipsIds = if (showRelationShips) p.relationships else Set.empty,
      kind = p.kind.toApi,
      createdAt = p.createdAt
    )
  }

  implicit class ReadModelClientWithKeysWrapper(private val rmck: ReadModelClientWithKeys) extends AnyVal {
    def toApi(showRelationShips: Boolean): ClientWithKeys =
      ClientWithKeys(
        client = Client(
          id = rmck.id,
          name = rmck.name,
          description = rmck.description,
          consumerId = rmck.consumerId,
          purposes = rmck.purposes.map(p => ClientPurpose(states = p.toApi)),
          relationshipsIds = if (showRelationShips) rmck.relationships else Set.empty,
          kind = rmck.kind.toApi,
          createdAt = rmck.createdAt
        ),
        keys = rmck.keys.map(_.toApi)
      )
  }

  implicit class PersistentKeyWrapper(private val k: PersistentKey) extends AnyVal {
    def toApi: Key =
      Key(
        kid = k.kid,
        encodedPem = k.encodedPem,
        use = k.use.toApi,
        algorithm = k.algorithm,
        name = k.name,
        createdAt = k.createdAt,
        relationshipId = k.relationshipId
      )
  }

  implicit class ManagementClientPurposeWrapper(private val cp: AuthorizationManagementDependency.Purpose)
      extends AnyVal {
    def toApi: ClientPurpose = ClientPurpose(states = cp.states.toApi)
  }

  implicit class ManagementClientWrapper(private val p: AuthorizationManagementDependency.Client) extends AnyVal {
    def toApi(showRelationShips: Boolean): Client = Client(
      id = p.id,
      name = p.name,
      description = p.description,
      consumerId = p.consumerId,
      purposes = p.purposes.map(_.toApi),
      relationshipsIds = if (showRelationShips) p.relationships else Set.empty,
      kind = p.kind.toApi,
      createdAt = p.createdAt
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

  implicit class ClientEServiceDetailsWrapper(private val pced: PersistentClientEServiceDetails) extends AnyVal {
    def toApi: ClientEServiceDetails = ClientEServiceDetails(
      descriptorId = pced.descriptorId,
      eserviceId = pced.eServiceId,
      audience = pced.audience,
      voucherLifespan = pced.voucherLifespan,
      state = pced.state.toApi
    )
  }

  implicit class ClientAgreementDetailsWrapper(private val pcad: PersistentClientAgreementDetails) extends AnyVal {
    def toApi: ClientAgreementDetails =
      ClientAgreementDetails(
        eserviceId = pcad.eServiceId,
        consumerId = pcad.consumerId,
        agreementId = pcad.agreementId,
        state = pcad.state.toApi
      )
  }

  implicit class KeySeedWrapper(private val keySeed: KeySeed) extends AnyVal {
    def toDependency(relationshipId: UUID, createdAt: OffsetDateTime): AuthorizationManagementDependency.KeySeed =
      AuthorizationManagementDependency.KeySeed(
        relationshipId = relationshipId,
        key = keySeed.key,
        use = keySeed.use.toDependency,
        alg = keySeed.alg,
        name = keySeed.name,
        createdAt = createdAt
      )
  }

  implicit class KeyUseWrapper(private val use: ProcessKeyUse) extends AnyVal {
    def toDependency: AuthorizationManagementDependency.KeyUse = use match {
      case ProcessKeyUse.SIG => AuthorizationManagementDependency.KeyUse.SIG
      case ProcessKeyUse.ENC => AuthorizationManagementDependency.KeyUse.ENC
    }
  }

  implicit class PersistentKeyUseWrapper(private val use: PersistentKeyUse) extends AnyVal {
    def toApi: ProcessKeyUse = use match {
      case Sig => ProcessKeyUse.SIG
      case Enc => ProcessKeyUse.ENC
    }
  }

  implicit class DependencyKeyUseWrapper(private val use: AuthorizationManagementDependency.KeyUse) extends AnyVal {
    def toApi: ProcessKeyUse = use match {
      case KeyUse.SIG => ProcessKeyUse.SIG
      case KeyUse.ENC => ProcessKeyUse.ENC
    }
  }

  implicit class KeyWrapper(private val key: AuthorizationManagementDependency.Key) extends AnyVal {
    def toApi: Key =
      Key(
        kid = key.kid,
        encodedPem = key.encodedPem,
        algorithm = key.algorithm,
        use = key.use.toApi,
        name = key.name,
        createdAt = key.createdAt,
        relationshipId = key.relationshipId
      )
  }

  implicit class OtherPrimeInfoWrapper(private val info: AuthorizationManagementDependency.OtherPrimeInfo)
      extends AnyVal {
    def toApi: OtherPrimeInfo = OtherPrimeInfo(r = info.r, d = info.d, t = info.t)
  }

  implicit class ClientPurposeDetailsWrapper(private val pcpd: PersistentClientPurposeDetails) extends AnyVal {
    def toApi: ClientPurposeDetails =
      ClientPurposeDetails(purposeId = pcpd.purposeId, versionId = pcpd.versionId, state = pcpd.state.toApi)
  }

  implicit class ManagementPurposeDetailsWrapper(
    private val cpd: AuthorizationManagementDependency.ClientPurposeDetails
  ) extends AnyVal {
    def toApi: ClientPurposeDetails =
      ClientPurposeDetails(purposeId = cpd.purposeId, versionId = cpd.versionId, state = cpd.state.toApi)
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
      descriptorId = ced.descriptorId,
      audience = ced.audience,
      voucherLifespan = ced.voucherLifespan,
      state = ced.state.toApi
    )
  }

  implicit class ManagementClientAgreementDetailsWrapper(
    private val cad: AuthorizationManagementDependency.ClientAgreementDetails
  ) extends AnyVal {
    def toApi: ClientAgreementDetails =
      ClientAgreementDetails(
        eserviceId = cad.eserviceId,
        consumerId = cad.consumerId,
        agreementId = cad.agreementId,
        state = cad.state.toApi
      )
  }

  implicit class ManagementClientComponentStateWrapper(
    private val ck: AuthorizationManagementDependency.ClientComponentState
  ) extends AnyVal {
    def toApi: ClientComponentState = ck match {
      case AuthorizationManagementDependency.ClientComponentState.ACTIVE   => ClientComponentState.ACTIVE
      case AuthorizationManagementDependency.ClientComponentState.INACTIVE => ClientComponentState.INACTIVE
    }
  }

  implicit class JwkOtherPrimeInfoWrapper(private val o: JwkOtherPrimeInfo) extends AnyVal {
    def toApi: OtherPrimeInfo = OtherPrimeInfo(r = o.r, d = o.d, t = o.t)
  }
}
