package it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl

import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{PartyManagementInvoker, PartyManagementService}
import it.pagopa.pdnd.interop.uservice.partymanagement.client.api.PartyApi
import it.pagopa.pdnd.interop.uservice.partymanagement.client.invoker.ApiRequest
import it.pagopa.pdnd.interop.uservice.partymanagement.client.model.Organization
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class PartyManagementServiceImpl(invoker: PartyManagementInvoker, api: PartyApi)(implicit ec: ExecutionContext)
    extends PartyManagementService {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def getOrganization(organizationId: UUID): Future[Organization] = {
    val request: ApiRequest[Organization] = api.getPartyOrganizationByUUID(organizationId)
    invoker
      .execute[Organization](request)
      .map { x =>
        logger.info(s"Retrieving Organization status code > ${x.code.toString}")
        logger.info(s"Retrieving Organization content > ${x.content.toString}")
        x.content
      }
      .recoverWith { case ex =>
        logger.error(s"Retrieving Organization, error > ${ex.getMessage}")
        Future.failed[Organization](ex)
      }
  }

}
