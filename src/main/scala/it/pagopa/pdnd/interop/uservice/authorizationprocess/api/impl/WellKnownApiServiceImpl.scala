package it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import cats.implicits._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.WellKnownApiService
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service._
import it.pagopa.pdnd.interop.uservice.partymanagement.client.model.{Problem => _}
import spray.json._

import scala.util.{Failure, Success, Try}

@SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.Product"))
final case class WellKnownApiServiceImpl(vaultService: VaultService) extends WellKnownApiService {

  /** Code: 200, Message: PDND public keys in JWK format., DataType: KeysResponse
    * Code: 400, Message: Bad Request, DataType: Problem
    */
  override def getWellKnownKeys()(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerKeysResponse: ToEntityMarshaller[KeysResponse],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val rsaPublicKey: Try[Map[String, String]] = {
      val path = VaultService.extractKeyPath("rsa", "public")
      path.map(vaultService.getSecret)
    }

    val ecPublicKey: Try[Map[String, String]] = {
      val path = VaultService.extractKeyPath("ec", "public")
      path.map(vaultService.getSecret)
    }

    val result: Try[Seq[Key]] = for {
      rsaKeys <- rsaPublicKey
      ecKeys  <- ecPublicKey
      keys    <- (rsaKeys ++ ecKeys).toSeq.traverse(Function.tupled(convertToKey))
    } yield keys

    result match {
      case Success(keys) => getWellKnownKeys200(KeysResponse(keys))
      case Failure(ex) =>
        getWellKnownKeys400(Problem(Option(ex.getMessage), 400, "Something goes wrong trying to get well-known keys"))
    }

  }

  private def convertToKey(kid: String, key: String): Try[Key] = Try {
    val mapped: Map[String, JsValue] = key.parseJson.asJsObject.fields + ("kid" -> JsString(kid))
    mapped.toJson.convertTo[Key]
  }

}