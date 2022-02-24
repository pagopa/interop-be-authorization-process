package it.pagopa.interop.authorizationprocess.service

import it.pagopa.interop.authorizationmanagement.client.model._
import it.pagopa.interop.authorizationprocess.model.{
  ClientAgreementDetails => ApiClientAgreementDetails,
  ClientComponentState => ApiClientComponentState,
  ClientEServiceDetails => ApiClientEServiceDetails,
  ClientKey => ApiClientKey,
  ClientKind => ApiClientKind,
  ClientPurposeDetails => ApiClientPurposeDetails,
  ClientStatesChain => ApiClientStatesChain,
  Key => ApiKey,
  KeySeed => ApiKeySeed,
  KeyUse => ApiKeyUse,
  OtherPrimeInfo => ApiOtherPrimeInfo,
  Purpose => ApiPurpose
}

import java.util.UUID
import scala.concurrent.Future

trait AuthorizationManagementService {

  def createClient(consumerId: UUID, name: String, description: Option[String], kind: ClientKind)(
    bearer: String
  ): Future[ManagementClient]

  def getClient(clientId: UUID)(bearer: String): Future[ManagementClient]
  def listClients(
    offset: Option[Int],
    limit: Option[Int],
    relationshipId: Option[UUID],
    consumerId: Option[UUID],
    purposeId: Option[UUID],
    kind: Option[ClientKind]
  )(bearer: String): Future[Seq[ManagementClient]]

  def deleteClient(clientId: UUID)(bearer: String): Future[Unit]

  def addRelationship(clientId: UUID, relationshipId: UUID)(bearer: String): Future[ManagementClient]
  def removeClientRelationship(clientId: UUID, relationshipId: UUID)(bearer: String): Future[Unit]

  def getKey(clientId: UUID, kid: String)(bearer: String): Future[ClientKey]
  def getClientKeys(clientId: UUID)(bearer: String): Future[KeysResponse]
  def createKeys(clientId: UUID, keysSeeds: Seq[KeySeed])(bearer: String): Future[KeysResponse]
  def deleteKey(clientId: UUID, kid: String)(bearer: String): Future[Unit]
  def getEncodedClientKey(clientId: UUID, kid: String)(bearer: String): Future[EncodedClientKey]

  def addClientPurpose(clientId: UUID, purposeSeed: PurposeSeed)(bearer: String): Future[Purpose]
  def removeClientPurpose(clientId: UUID, purposeId: UUID)(bearer: String): Future[Unit]
}

object AuthorizationManagementService {

  def keyToApi(clientKey: ClientKey): ApiClientKey = {
    import clientKey.key
    ApiClientKey(
      name = clientKey.name,
      createdAt = clientKey.createdAt,
      key = ApiKey(
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
        oth = key.oth.map(_.map(primeInfoToApi))
      )
    )
  }

  def primeInfoToApi(info: OtherPrimeInfo): ApiOtherPrimeInfo =
    ApiOtherPrimeInfo(r = info.r, d = info.d, t = info.t)

  def toDependencyKeySeed(keySeed: ApiKeySeed, relationshipId: UUID): KeySeed =
    KeySeed(
      relationshipId = relationshipId,
      key = keySeed.key,
      use = keyUseToDependency(keySeed.use),
      alg = keySeed.alg,
      name = keySeed.name
    )

  def keyUseToDependency(use: ApiKeyUse): KeyUse =
    use match {
      case ApiKeyUse.SIG => KeyUse.SIG
      case ApiKeyUse.ENC => KeyUse.ENC
    }

  def purposeToApi(purpose: Purpose): ApiPurpose =
    ApiPurpose(purposeId = purpose.purposeId, states = clientStatesChainToApi(purpose.states))

  def clientStatesChainToApi(states: ClientStatesChain): ApiClientStatesChain =
    ApiClientStatesChain(
      id = states.id,
      eservice = clientEServiceDetailsToApi(states.eservice),
      agreement = clientAgreementDetailsToApi(states.agreement),
      purpose = clientPurposeDetailsToApi(states.purpose)
    )

  def clientEServiceDetailsToApi(details: ClientEServiceDetails): ApiClientEServiceDetails =
    ApiClientEServiceDetails(
      eserviceId = details.eserviceId,
      state = clientComponentStateToApi(details.state),
      audience = details.audience,
      voucherLifespan = details.voucherLifespan
    )

  def clientAgreementDetailsToApi(details: ClientAgreementDetails): ApiClientAgreementDetails =
    ApiClientAgreementDetails(agreementId = details.agreementId, state = clientComponentStateToApi(details.state))

  def clientPurposeDetailsToApi(details: ClientPurposeDetails): ApiClientPurposeDetails =
    ApiClientPurposeDetails(purposeId = details.purposeId, state = clientComponentStateToApi(details.state))

  def clientComponentStateToApi(state: ClientComponentState): ApiClientComponentState =
    state match {
      case ClientComponentState.ACTIVE   => ApiClientComponentState.ACTIVE
      case ClientComponentState.INACTIVE => ApiClientComponentState.INACTIVE
    }

  def convertToApiClientKind(kind: ClientKind): ApiClientKind =
    kind match {
      case ClientKind.CONSUMER => ApiClientKind.CONSUMER
      case ClientKind.API      => ApiClientKind.API
    }

  def convertFromApiClientKind(kind: ApiClientKind): ClientKind =
    kind match {
      case ApiClientKind.CONSUMER => ClientKind.CONSUMER
      case ApiClientKind.API      => ClientKind.API
    }
}
