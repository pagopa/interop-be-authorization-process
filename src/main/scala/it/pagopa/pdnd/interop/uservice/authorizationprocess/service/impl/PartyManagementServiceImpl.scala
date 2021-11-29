package it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl

import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{PartyManagementInvoker, PartyManagementService}
import it.pagopa.pdnd.interop.uservice.partymanagement.client.invoker.BearerToken
import it.pagopa.pdnd.interop.uservice.partymanagement.client.api.PartyApi
import it.pagopa.pdnd.interop.uservice.partymanagement.client.invoker.ApiRequest
import it.pagopa.pdnd.interop.uservice.partymanagement.client.model.{
  Organization,
  Person,
  PersonSeed,
  Relationship,
  RelationshipSeed,
  Relationships
}
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class PartyManagementServiceImpl(invoker: PartyManagementInvoker, api: PartyApi)(implicit ec: ExecutionContext)
    extends PartyManagementService {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def getOrganization(organizationId: UUID)(bearerToken: String): Future[Organization] = {
    val request: ApiRequest[Organization] = api.getOrganizationById(organizationId)(BearerToken(bearerToken))
    invoke(request, "Retrieve Organization")
  }

  override def getPerson(personId: UUID)(bearerToken: String): Future[Person] = {
    val request: ApiRequest[Person] = api.getPersonById(personId)(BearerToken(bearerToken))
    invoke(request, "Retrieve Person")
  }

  override def getRelationships(organizationId: UUID, personId: UUID, platformRole: String)(
    bearerToken: String
  ): Future[Relationships] = {
    val request: ApiRequest[Relationships] =
      api.getRelationships(Some(personId), Some(organizationId), Some(platformRole))(BearerToken(bearerToken))
    invoke(request, "Retrieve Relationships")
  }

  override def getRelationshipsByPersonId(personId: UUID, platformRole: Option[String])(
    bearerToken: String
  ): Future[Relationships] = {
    val request: ApiRequest[Relationships] =
      api.getRelationships(Some(personId), None, platformRole)(BearerToken(bearerToken))
    invoke(request, "Retrieve Relationships By Person Id")
  }

  override def getRelationshipById(relationshipId: UUID)(bearerToken: String): Future[Relationship] = {
    val request: ApiRequest[Relationship] = api.getRelationshipById(relationshipId)(BearerToken(bearerToken))
    invoke(request, "Retrieve Relationship By Id")
  }

  def createPerson(seed: PersonSeed)(bearerToken: String): Future[Person] = {
    val request: ApiRequest[Person] = api.createPerson(seed)(BearerToken(bearerToken))
    invoke(request, "Creating Person")
  }

  def createRelationship(seed: RelationshipSeed)(bearerToken: String): Future[Relationship] = {
    val createRequest: ApiRequest[Relationship] = api.createRelationship(seed)(BearerToken(bearerToken))
    invoke(createRequest, "Creating Relationship")
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
