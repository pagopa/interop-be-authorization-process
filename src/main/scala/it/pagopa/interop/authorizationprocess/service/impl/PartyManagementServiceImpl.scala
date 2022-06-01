package it.pagopa.interop.authorizationprocess.service.impl

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.authorizationprocess.service.{
  PartyManagementApiKeyValue,
  PartyManagementInvoker,
  PartyManagementService
}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.AkkaUtils.getUidFuture
import it.pagopa.interop.commons.utils.INTEROP_PRODUCT_NAME
import it.pagopa.interop.selfcare.partymanagement.client.api.PartyApi
import it.pagopa.interop.selfcare.partymanagement.client.model._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

final case class PartyManagementServiceImpl(invoker: PartyManagementInvoker, api: PartyApi)(implicit
  partyManagementApiKeyValue: PartyManagementApiKeyValue
) extends PartyManagementService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getInstitution(
    institutionId: UUID
  )(implicit contexts: Seq[(String, String)], ec: ExecutionContext): Future[Institution] =
    for {
      uid <- getUidFuture(contexts)
      request = api.getInstitutionById(institutionId)(uid)
      result <- invoker.invoke(request, "Retrieve Institution")
    } yield result

  override def getRelationships(organizationId: UUID, personId: UUID, productRoles: Seq[String])(implicit
    contexts: Seq[(String, String)],
    ec: ExecutionContext
  ): Future[Relationships] = for {
    uid <- getUidFuture(contexts)
    request = api.getRelationships(
      from = Some(personId),
      to = Some(organizationId),
      roles = Seq.empty,
      states = Seq.empty,
      products = Seq(INTEROP_PRODUCT_NAME),
      productRoles = productRoles
    )(uid)
    result <- invoker.invoke(request, "Retrieve Relationships")
  } yield result

  override def getRelationshipsByPersonId(personId: UUID, productRoles: Seq[String])(implicit
    contexts: Seq[(String, String)],
    ec: ExecutionContext
  ): Future[Relationships] = for {
    uid <- getUidFuture(contexts)
    request = api.getRelationships(
      from = Some(personId),
      to = None,
      roles = Seq.empty,
      states = Seq.empty,
      products = Seq(INTEROP_PRODUCT_NAME),
      productRoles = productRoles
    )(uid)
    result <- invoker.invoke(request, "Retrieve Relationships By Person Id")
  } yield result

  override def getRelationshipById(
    relationshipId: UUID
  )(implicit contexts: Seq[(String, String)], ec: ExecutionContext): Future[Relationship] = for {
    uid <- getUidFuture(contexts)
    request = api.getRelationshipById(relationshipId)(uid)
    result <- invoker.invoke(request, "Retrieve Relationship By Id")
  } yield result

}
