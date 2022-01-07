package it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import cats.implicits._
import com.typesafe.scalalogging.Logger
import it.pagopa.pdnd.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.pdnd.interop.commons.vault.service.VaultService
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.WellKnownApiService
import it.pagopa.pdnd.interop.uservice.authorizationprocess.error.AuthorizationProcessErrors.WellKnownRetrievalError
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service._
import it.pagopa.pdnd.interop.uservice.partymanagement.client.model.{Problem => _}
import org.slf4j.LoggerFactory
import spray.json._

import scala.util.{Failure, Success, Try}

final case class WellKnownApiServiceImpl(vaultService: VaultService) extends WellKnownApiService {

  val logger = Logger.takingImplicit[ContextFieldsToLog](LoggerFactory.getLogger(this.getClass))

  /** Code: 200, Message: PDND public keys in JWK format., DataType: KeysResponse
    * Code: 400, Message: Bad Request, DataType: Problem
    */
  override def getWellKnownKeys()(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerKeysResponse: ToEntityMarshaller[KeysResponse],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    logger.info("Getting well-known keys")
    val rsaPublicKey: Try[Map[String, String]] = Try {
      val path = VaultSecretPaths.extractPublicKeysPath("rsa")
      vaultService.readBase64EncodedData(path)
    }

    val ecPublicKey: Try[Map[String, String]] = Try {
      val path = VaultSecretPaths.extractPublicKeysPath("ec")
      vaultService.readBase64EncodedData(path)
    }

    val result: Try[Seq[Key]] = for {
      rsaKeys <- rsaPublicKey
      ecKeys  <- ecPublicKey
      keys    <- (rsaKeys ++ ecKeys).toSeq.traverse(Function.tupled(convertToKey))
    } yield keys

    result match {
      case Success(keys) => getWellKnownKeys200(KeysResponse(keys))
      case Failure(ex) =>
        logger.error("Error while getting well-known keys", ex)
        getWellKnownKeys400(problemOf(StatusCodes.BadRequest, WellKnownRetrievalError))
    }

  }

  private def convertToKey(kid: String, key: String): Try[Key] = Try {
    val mapped: Map[String, JsValue] = key.parseJson.asJsObject.fields + ("kid" -> JsString(kid))
    mapped.toJson.convertTo[Key]
  }

}
