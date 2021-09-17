package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import it.pagopa.pdnd.interop.uservice.authorizationprocess.error.EnumParameterError
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.{
  ClientKey => ApiClientKey,
  Key => ApiKey,
  KeySeed => ApiKeySeed,
  OtherPrimeInfo => ApiOtherPrimeInfo
}
import it.pagopa.pdnd.interop.uservice.keymanagement.client.model._

import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

trait AuthorizationManagementService {

  def createClient(
    eServiceId: UUID,
    consumerId: UUID,
    name: String,
    description: Option[String]
  ): Future[ManagementClient]
  def getClient(clientId: String): Future[ManagementClient]
  def listClients(
    offset: Option[Int],
    limit: Option[Int],
    eServiceId: Option[UUID],
    operatorId: Option[UUID]
  ): Future[Seq[ManagementClient]]
  def deleteClient(clientId: String): Future[Unit]

  def addOperator(clientId: UUID, operatorId: UUID): Future[ManagementClient]
  def removeClientOperator(clientId: UUID, operatorId: UUID): Future[Unit]

  def getKey(clientId: UUID, kid: String): Future[ClientKey]
  def getClientKeys(clientId: UUID): Future[KeysResponse]
  def createKeys(clientId: UUID, keysSeeds: Seq[KeySeed]): Future[KeysResponse]
  def deleteKey(clientId: UUID, kid: String): Future[Unit]
  def enableKey(clientId: UUID, kid: String): Future[Unit]
  def disableKey(clientId: UUID, kid: String): Future[Unit]
}

object AuthorizationManagementService {

  def keyToApi(clientKey: ClientKey): ApiClientKey = {
    import clientKey.key
    ApiClientKey(
      status = clientKey.status.toString,
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

  def toClientKeySeed(keySeed: ApiKeySeed): Either[EnumParameterError, KeySeed] =
    Try(KeySeedEnums.Use.withName(keySeed.use)).toEither
      .map(use => KeySeed(operatorId = keySeed.operatorId, key = keySeed.key, use = use, alg = keySeed.alg))
      .left
      .map(_ => EnumParameterError("use", KeySeedEnums.Use.values.toSeq.map(_.toString)))

}
