package it.pagopa.interop.authorizationprocess.common

import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationmanagement.model.client._
import it.pagopa.interop.authorizationmanagement.model.client.PersistentClientComponentState.Active
import it.pagopa.interop.authorizationmanagement.model.client.PersistentClientComponentState.Inactive
import it.pagopa.interop.authorizationmanagement.model.client.{Api, Consumer}
import it.pagopa.interop.authorizationmanagement.client.{model => AuthorizationManagementDependency}
import java.util.UUID

object Adapters {

  implicit class PersistentClientWrapper(private val p: PersistentClient) extends AnyVal {
    def toApi(showRelationShips: Boolean): Client = Client(
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
    def toApi(showRelationShips: Boolean): Client = Client(
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
    def toDependency(relationshipId: UUID): AuthorizationManagementDependency.KeySeed =
      AuthorizationManagementDependency.KeySeed(
        relationshipId = relationshipId,
        key = keySeed.key,
        use = keySeed.use.toDependency,
        alg = keySeed.alg,
        name = keySeed.name
      )
  }

  implicit class KeyUseWrapper(private val use: KeyUse) extends AnyVal {
    def toDependency: AuthorizationManagementDependency.KeyUse = use match {
      case KeyUse.SIG => AuthorizationManagementDependency.KeyUse.SIG
      case KeyUse.ENC => AuthorizationManagementDependency.KeyUse.ENC
    }
  }

  implicit class ClientKeyWrapper(private val k: AuthorizationManagementDependency.ClientKey) extends AnyVal {
    def toApi: ClientKey                          =
      ClientKey(name = k.name, createdAt = k.createdAt, key = k.key.toApi)
    def toReadKeyApi(op: Operator): ReadClientKey =
      ReadClientKey(
        name = k.name,
        createdAt = k.createdAt,
        operator = OperatorDetails(op.relationshipId, op.name, op.familyName),
        key = k.key.toApi
      )
  }

  implicit class KeyWrapper(private val key: AuthorizationManagementDependency.Key) extends AnyVal {
    def toApi: Key = Key(
      kty = key.kty,
      key_ops = key.keyOps,
      use = key.use,
      alg = key.alg,
      kid = key.kid,
      x5u = key.x5u,
      x5t = key.x5t,
      x5tS256 = key.x5tS256,
      x5c = key.x5c,
      crv = key.crv,
      x = key.x,
      y = key.y,
      d = key.d,
      k = key.k,
      n = key.n,
      e = key.e,
      p = key.p,
      q = key.q,
      dp = key.dp,
      dq = key.dq,
      qi = key.qi,
      oth = key.oth.map(_.map(_.toApi))
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
}
