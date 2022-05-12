package it.pagopa.interop.authorizationprocess.service.impl

import it.pagopa.interop.authorizationprocess.service.{PartyManagementInvoker, PartyManagementService}
import it.pagopa.interop.partymanagement.client.api.PartyApi
import it.pagopa.interop.partymanagement.client.invoker.{ApiRequest, BearerToken}
import it.pagopa.interop.partymanagement.client.model._
import it.pagopa.interop.commons.utils.INTEROP_PRODUCT_NAME
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.Future

final case class PartyManagementServiceImpl(invoker: PartyManagementInvoker, api: PartyApi)
    extends PartyManagementService {

  implicit val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def getInstitution(
    institutionId: UUID
  )(bearerToken: String)(implicit contexts: Seq[(String, String)]): Future[Institution] = {
    val request: ApiRequest[Institution] = api.getInstitutionById(institutionId)(BearerToken(bearerToken))
    invoker.invoke(request, "Retrieve Institution")
  }

  override def getRelationships(organizationId: UUID, personId: UUID, productRoles: Seq[String])(
    bearerToken: String
  )(implicit contexts: Seq[(String, String)]): Future[Relationships] = {
    val request: ApiRequest[Relationships] =
      api.getRelationships(
        from = Some(personId),
        to = Some(organizationId),
        roles = Seq.empty,
        states = Seq.empty,
        products = Seq(INTEROP_PRODUCT_NAME),
        productRoles = productRoles
      )(BearerToken(bearerToken))
    invoker.invoke(request, "Retrieve Relationships")
  }

  override def getRelationshipsByPersonId(personId: UUID, productRoles: Seq[String])(
    bearerToken: String
  )(implicit contexts: Seq[(String, String)]): Future[Relationships] = {
    val request: ApiRequest[Relationships] =
      api.getRelationships(
        from = Some(personId),
        to = None,
        roles = Seq.empty,
        states = Seq.empty,
        products = Seq(INTEROP_PRODUCT_NAME),
        productRoles = productRoles
      )(BearerToken(bearerToken))
    invoker.invoke(request, "Retrieve Relationships By Person Id")
  }

  override def getRelationshipById(
    relationshipId: UUID
  )(bearerToken: String)(implicit contexts: Seq[(String, String)]): Future[Relationship] = {
    val request: ApiRequest[Relationship] = api.getRelationshipById(relationshipId)(BearerToken(bearerToken))
    invoker.invoke(request, "Retrieve Relationship By Id")
  }

}
