package it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl

import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{
  AuthorizationManagementService,
  KeyManagementInvoker
}
import it.pagopa.pdnd.interop.uservice.keymanagement.client.api.ClientApi
import it.pagopa.pdnd.interop.uservice.keymanagement.client.invoker.ApiRequest
import it.pagopa.pdnd.interop.uservice.keymanagement.client.model.{Client, ClientSeed, OperatorSeed}
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
      .map { response =>
        logger.debug(s"Creating client content > ${response.content.toString}")
        response.content
      }
      .recoverWith { case ex =>
        logger.error(s"Creating client, error > ${ex.getMessage}")
        Future.failed[Client](ex)
      }
  }

  override def getClient(clientId: String): Future[Client] = {
    val request: ApiRequest[Client] = api.getClient(clientId)
    invoker
      .execute[Client](request)
      .map { x =>
        logger.debug(s"Retrieved client content > ${x.content.toString}")
        x.content
      }
      .recoverWith { case ex =>
        logger.error(s"Retrieve client, error > ${ex.getMessage}")
        Future.failed[Client](ex)
      }
  }

  override def listClients(
    offset: Option[Int],
    limit: Option[Int],
    agreementId: Option[UUID],
    operatorId: Option[UUID]
  ): Future[Seq[Client]] = {
    val request: ApiRequest[Seq[Client]] = api.listClients(offset, limit, agreementId, operatorId)
    invoker
      .execute[Seq[Client]](request)
      .map { response =>
        logger.debug(s"Listing clients content > ${response.content.toString}")
        response.content
      }
      .recoverWith { case ex =>
        logger.error(s"Listing clients, error > ${ex.getMessage}")
        Future.failed[Seq[Client]](ex)
      }
  }

  override def deleteClient(clientId: String): Future[Unit] = {
    val request: ApiRequest[Unit] = api.deleteClient(clientId)
    invoker
      .execute[Unit](request)
      .map { response =>
        logger.debug(s"Client deleted content")
        response.content
      }
      .recoverWith { case ex =>
        logger.error(s"Delete client, error > ${ex.getMessage}")
        Future.failed[Unit](ex)
      }
  }


  override def addOperator(clientId: UUID, operatorId: UUID): Future[Client] = {
    val request: ApiRequest[Client] = api.addOperator(clientId, OperatorSeed(operatorId))
    invoker
      .execute[Client](request)
      .map { response =>
        logger.debug(s"Adding operator to client content > ${response.content.toString}")
        response.content
      }
      .recoverWith { case ex =>
        logger.error(s"Adding operator to client, error > ${ex.getMessage}")
        Future.failed[Client](ex)
      }
  }
}
