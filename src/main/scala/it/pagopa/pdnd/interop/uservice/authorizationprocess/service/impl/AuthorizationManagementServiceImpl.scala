package it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl

import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{
  AuthorizationManagementService,
  KeyManagementInvoker
}
import it.pagopa.pdnd.interop.uservice.keymanagement.client.api.ClientApi
import it.pagopa.pdnd.interop.uservice.keymanagement.client.invoker.ApiRequest
import it.pagopa.pdnd.interop.uservice.keymanagement.client.model.{Client, ClientSeed}
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class AuthorizationManagementServiceImpl(invoker: KeyManagementInvoker, api: ClientApi)(implicit ec: ExecutionContext)
    extends AuthorizationManagementService {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /** Returns the expected audience defined by the producer of the corresponding agreementId.
    *
    * @param agreementId
    * @param description
    * @return
    */
  override def createClient(agreementId: UUID, description: String): Future[Client] = {
    val request: ApiRequest[Client] = api.createClient(ClientSeed(agreementId, description))
    invoker
      .execute[Client](request)
      .map { x =>
        logger.info(s"Creating client content > ${x.content.toString}")
        x.content
      }
      .recoverWith { case ex =>
        logger.error(s"Creating client, error > ${ex.getMessage}")
        Future.failed[Client](ex)
      }
  }
}
