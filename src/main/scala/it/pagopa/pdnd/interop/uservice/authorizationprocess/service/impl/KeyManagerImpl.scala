package it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl

import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.KeyManager
import it.pagopa.pdnd.interop.uservice.keymanagement.client.api.KeyApi
import it.pagopa.pdnd.interop.uservice.keymanagement.client.invoker.{ApiInvoker, ApiRequest}
import it.pagopa.pdnd.interop.uservice.keymanagement.client.model.Key
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

final case class KeyManagerImpl(invoker: ApiInvoker, api: KeyApi)(implicit ec: ExecutionContext) extends KeyManager {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  override def getKey(clientId: String, kid: String): Future[Key] = {
    val request: ApiRequest[Key] = api.getPartyKeyById(UUID.fromString(clientId), kid)
    invoker
      .execute[Key](request)
      .map { x =>
        logger.info(s"Retrieving key ${x.code.toString}")
        logger.info(s"Retrieving key ${x.content.toString}")
        x.content
      }
      .recoverWith { case ex =>
        logger.error(s"Retrieving key ${ex.getMessage}")
        Future.failed[Key](ex)
      }
  }

}
