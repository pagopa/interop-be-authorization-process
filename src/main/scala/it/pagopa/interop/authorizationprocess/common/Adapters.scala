package it.pagopa.interop.authorizationprocess.common

import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationprocess.model.{KeyUse => ProcessKeyUse}
import it.pagopa.interop.authorizationmanagement.model.client._
import it.pagopa.interop.authorizationmanagement.model.client.PersistentClientComponentState.Active
import it.pagopa.interop.authorizationmanagement.model.client.PersistentClientComponentState.Inactive
import it.pagopa.interop.authorizationmanagement.model.client.{Api, Consumer}
import it.pagopa.interop.authorizationmanagement.model.key.{Enc, Sig}
import it.pagopa.interop.authorizationmanagement.client.{model => AuthorizationManagementDependency}
import it.pagopa.interop.authorizationprocess.common.readmodel.model.ReadModelClientWithKeys
import it.pagopa.interop.authorizationmanagement.model.key.{PersistentKeyUse, PersistentKey}
import it.pagopa.interop.authorizationmanagement.processor.key.KeyProcessor
import com.nimbusds.jose.jwk._
import scala.jdk.CollectionConverters.{ListHasAsScala, SetHasAsScala}
import scala.annotation.nowarn

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
    def toApi: KeyEntry                                              =
      KeyEntry(
        id = k.kid,
        key = k.encodedPem,
        use = k.use.toApi,
        alg = k.algorithm,
        name = k.name,
        createdAt = k.createdAt,
        relationshipId = k.relationshipId
      )
    def toReadKeyApi(op: Operator): Either[Throwable, ReadClientKey] =
      fromBase64encodedPEMToAPIKey(k.kid, k.encodedPem, k.use, k.algorithm)
        .map(key =>
          ReadClientKey(
            name = k.name,
            createdAt = k.createdAt,
            operator = OperatorDetails(op.relationshipId, op.name, op.familyName),
            key = key
          )
        )

    def toClientKeyApi: Either[Throwable, ClientKey] =
      fromBase64encodedPEMToAPIKey(k.kid, k.encodedPem, k.use, k.algorithm)
        .map(key => ClientKey(name = k.name, createdAt = k.createdAt, key = key))
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
    def toRfcValue: String   = use match {
      case Sig => "sig"
      case Enc => "enc"
    }
  }

  implicit class ClientKeyWrapper(private val k: AuthorizationManagementDependency.ClientKey) extends AnyVal {
    def toApi: ClientKey =
      ClientKey(name = k.name, createdAt = k.createdAt, key = k.key.toApi)
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

  def fromBase64encodedPEMToAPIKey(
    kid: String,
    base64PEM: String,
    use: PersistentKeyUse,
    algorithm: String
  ): Either[Throwable, Key] = {
    val key = for {
      jwk <- KeyProcessor.fromBase64encodedPEM(base64PEM)
    } yield jwk.getKeyType match {
      case KeyType.RSA => rsa(kid, jwk.toRSAKey)
      case KeyType.EC  => ec(kid, jwk.toECKey)
      case KeyType.OKP => okp(kid, jwk.toOctetKeyPair)
      case KeyType.OCT => oct(kid, jwk.toOctetSequenceKey)
      case _           => throw new RuntimeException(s"Unknown KeyType ${jwk.getKeyType}")
    }

    key.map(_.copy(alg = Some(algorithm), use = Some(use.toRfcValue)))
  }

  private def rsa(kid: String, key: RSAKey): Key           = {
    val otherPrimes = Option(key.getOtherPrimes)
      .map(list =>
        list.asScala
          .map(entry =>
            OtherPrimeInfo(
              r = entry.getPrimeFactor.toString,
              d = entry.getFactorCRTExponent.toString,
              t = entry.getFactorCRTCoefficient.toString
            )
          )
          .toSeq
      )
      .filter(_.nonEmpty)

    Key(
      use = None,
      alg = None,
      kty = key.getKeyType.getValue,
      key_ops = Option(key.getKeyOperations).map(list => list.asScala.map(op => op.toString).toSeq),
      kid = kid,
      x5u = Option(key.getX509CertURL).map(_.toString),
      x5t = getX5T(key),
      x5tS256 = Option(key.getX509CertSHA256Thumbprint).map(_.toString),
      x5c = Option(key.getX509CertChain).map(list => list.asScala.map(op => op.toString).toSeq),
      crv = None,
      x = None,
      y = None,
      d = Option(key.getPrivateExponent).map(_.toString),
      k = None,
      n = Option(key.getModulus).map(_.toString),
      e = Option(key.getPublicExponent).map(_.toString),
      p = Option(key.getFirstPrimeFactor).map(_.toString),
      q = Option(key.getSecondPrimeFactor).map(_.toString),
      dp = Option(key.getFirstFactorCRTExponent).map(_.toString),
      dq = Option(key.getSecondFactorCRTExponent).map(_.toString),
      qi = Option(key.getFirstCRTCoefficient).map(_.toString),
      oth = otherPrimes
    )
  }
  private def ec(kid: String, key: ECKey): Key             = Key(
    use = None,
    alg = None,
    kty = key.getKeyType.getValue,
    key_ops = Option(key.getKeyOperations).map(list => list.asScala.map(op => op.toString).toSeq),
    kid = kid,
    x5u = Option(key.getX509CertURL).map(_.toString),
    x5t = getX5T(key),
    x5tS256 = Option(key.getX509CertSHA256Thumbprint).map(_.toString),
    x5c = Option(key.getX509CertChain).map(list => list.asScala.map(op => op.toString).toSeq),
    crv = Option(key.getCurve).map(_.toString),
    x = Option(key.getX).map(_.toString),
    y = Option(key.getY).map(_.toString),
    d = Option(key.getD).map(_.toString),
    k = None,
    n = None,
    e = None,
    p = None,
    q = None,
    dp = None,
    dq = None,
    qi = None,
    oth = None
  )
  private def okp(kid: String, key: OctetKeyPair): Key     = {
    Key(
      use = None,
      alg = None,
      kty = key.getKeyType.getValue,
      key_ops = Option(key.getKeyOperations).map(list => list.asScala.map(op => op.toString).toSeq),
      kid = kid,
      x5u = Option(key.getX509CertURL).map(_.toString),
      x5t = getX5T(key),
      x5tS256 = Option(key.getX509CertSHA256Thumbprint).map(_.toString),
      x5c = Option(key.getX509CertChain).map(list => list.asScala.map(op => op.toString).toSeq),
      crv = Option(key.getCurve).map(_.toString),
      x = Option(key.getX).map(_.toString),
      y = None,
      d = Option(key.getD).map(_.toString),
      k = None,
      n = None,
      e = None,
      p = None,
      q = None,
      dp = None,
      dq = None,
      qi = None,
      oth = None
    )
  }
  private def oct(kid: String, key: OctetSequenceKey): Key = {
    Key(
      use = None,
      alg = None,
      kty = key.getKeyType.getValue,
      key_ops = Option(key.getKeyOperations).map(list => list.asScala.map(op => op.toString).toSeq),
      kid = kid,
      x5u = Option(key.getX509CertURL).map(_.toString),
      x5t = getX5T(key),
      x5tS256 = Option(key.getX509CertSHA256Thumbprint).map(_.toString),
      x5c = Option(key.getX509CertChain).map(list => list.asScala.map(op => op.toString).toSeq),
      crv = None,
      x = None,
      y = None,
      d = None,
      k = Option(key.getKeyValue).map(_.toString),
      n = None,
      e = None,
      p = None,
      q = None,
      dp = None,
      dq = None,
      qi = None,
      oth = None
    )

  }

  // encapsulating in a method to avoid compilation errors because of Nimbus deprecated method
  @nowarn
  @inline private def getX5T(key: JWK): Option[String] = Option(key.getX509CertThumbprint).map(_.toString)
}
