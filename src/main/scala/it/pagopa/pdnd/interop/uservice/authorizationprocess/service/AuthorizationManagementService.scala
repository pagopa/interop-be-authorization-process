package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.{
  ClientKey => ApiClientKey,
  ClientState => ApiClientState,
  Key => ApiKey,
  KeySeed => ApiKeySeed,
  KeyUse => ApiKeyUse,
  OtherPrimeInfo => ApiOtherPrimeInfo
}
import it.pagopa.pdnd.interop.uservice.keymanagement.client.model._

import java.util.UUID
import scala.concurrent.Future

trait AuthorizationManagementService {

  def createClient(eServiceId: UUID, consumerId: UUID, name: String, purposes: String, description: Option[String])(
    bearer: String
  ): Future[ManagementClient]
  def getClient(clientId: UUID)(bearer: String): Future[ManagementClient]
  def listClients(
    offset: Option[Int],
    limit: Option[Int],
    eServiceId: Option[UUID],
    relationshipId: Option[UUID],
    consumerId: Option[UUID]
  )(bearer: String): Future[Seq[ManagementClient]]
  def deleteClient(clientId: UUID)(bearer: String): Future[Unit]
  def activateClient(clientId: UUID)(bearer: String): Future[Unit]
  def suspendClient(clientId: UUID)(bearer: String): Future[Unit]

  def addRelationship(clientId: UUID, relationshipId: UUID)(bearer: String): Future[ManagementClient]
  def removeClientRelationship(clientId: UUID, relationshipId: UUID)(bearer: String): Future[Unit]

  def getKey(clientId: UUID, kid: String)(bearer: String): Future[ClientKey]
  def getClientKeys(clientId: UUID)(bearer: String): Future[KeysResponse]
  def createKeys(clientId: UUID, keysSeeds: Seq[KeySeed])(bearer: String): Future[KeysResponse]
  def deleteKey(clientId: UUID, kid: String)(bearer: String): Future[Unit]
  def getEncodedClientKey(clientId: UUID, kid: String)(bearer: String): Future[EncodedClientKey]
}

object AuthorizationManagementService {

  def keyToApi(clientKey: ClientKey): ApiClientKey = {
    import clientKey.key
    ApiClientKey(key =
      ApiKey(
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
      alg = keySeed.alg
    )

  def keyUseToDependency(use: ApiKeyUse): KeyUse =
    use match {
      case ApiKeyUse.SIG => KeyUse.SIG
      case ApiKeyUse.ENC => KeyUse.ENC
    }

  def clientStateToApi(state: ClientState): ApiClientState =
    state match {
      case ClientState.ACTIVE    => ApiClientState.ACTIVE
      case ClientState.SUSPENDED => ApiClientState.SUSPENDED
    }

}
