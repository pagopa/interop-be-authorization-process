package it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.WellKnownApiService
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.utils.decodeBase64
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service._
import it.pagopa.pdnd.interop.uservice.partymanagement.client.model.{Problem => _}
import spray.json._

import scala.concurrent.ExecutionContext
import scala.util.Try

@SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.Product"))
final case class WellKnownApiServiceImp(vaultService: VaultService)(implicit ec: ExecutionContext)
    extends WellKnownApiService {

  private val keyRootPath: Try[String] = Try(System.getenv("PDND_INTEROP_PRIVATE_KEY"))

  /** Code: 200, Message: PDND public keys in JWK format., DataType: KeysResponse
    */
  override def getPublicKey()(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerKeysResponse: ToEntityMarshaller[KeysResponse]
  ): Route = {
    val rsaPrivateKey: Try[Map[String, String]] =
      keyRootPath.map { root =>
        val rsaPath = s"$root/rsa/jwk/public"
        getPrivateKeyFromVault(rsaPath)
      }

    rsaPrivateKey.map { privateKey =>
      privateKey.map(key => key._2.parseJson.convertTo[Key])
    }


  }

  private def getPrivateKeyFromVault(path: String): Map[String, String] = {
    vaultService
      .getSecret(path)
      .view
      .mapValues(decodeBase64)
      .toMap
  }
}
