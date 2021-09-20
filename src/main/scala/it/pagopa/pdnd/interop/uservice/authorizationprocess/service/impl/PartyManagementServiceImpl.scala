package it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl

import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{PartyManagementInvoker, PartyManagementService}
import it.pagopa.pdnd.interop.uservice.partymanagement.client.api.PartyApi
import it.pagopa.pdnd.interop.uservice.partymanagement.client.invoker.ApiRequest
import it.pagopa.pdnd.interop.uservice.partymanagement.client.model.{Organization, Person, Relationships}
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class PartyManagementServiceImpl(invoker: PartyManagementInvoker, api: PartyApi)(implicit ec: ExecutionContext)
    extends PartyManagementService {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def getOrganization(organizationId: UUID): Future[Organization] = {
    val request: ApiRequest[Organization] = api.getPartyOrganizationByUUID(organizationId)
    invoke(request, "Retrieve Organization")
  }

  override def getPerson(personId: UUID): Future[Person] = {
    val request: ApiRequest[Person] = api.getPartyPersonByUUID(personId)
    invoke(request, "Retrieve Person")
  }

  override def getRelationships(organizationId: String, personId: String): Future[Relationships] = {
    val request: ApiRequest[Relationships] = api.getRelationships(Some(personId), Some(organizationId))
    invoke(request, "Retrieve Relationships")
  }

  override def getPersonByTaxCode(taxCode: String): Future[Person] = {
    val request: ApiRequest[Person] = api.getPerson(taxCode)
    invoke(request, "Retrieve Person By Tax Code")
  }

  private def invoke[T](request: ApiRequest[T], logMessage: String)(implicit m: Manifest[T]): Future[T] =
    invoker
      .execute[T](request)
      .map { response =>
        logger.debug(s"$logMessage. Status code: ${response.code.toString}. Content: ${response.content.toString}")
        response.content
      }
      .recoverWith { case ex =>
        logger.error(s"$logMessage. Error: ${ex.getMessage}")
        Future.failed[T](ex)
      }
}
