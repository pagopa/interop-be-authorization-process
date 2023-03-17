package it.pagopa.interop.authorizationprocess.service.impl

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.authorizationprocess.common.system.ApplicationConfiguration
import it.pagopa.interop.authorizationprocess.service.{
  PartyManagementApiKeyValue,
  PartyManagementInvoker,
  PartyManagementService
}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.selfcare.partymanagement.client.api.PartyApi
import it.pagopa.interop.selfcare.partymanagement.client.model._
import it.pagopa.interop.commons.utils.withUid
import it.pagopa.interop.commons.utils.TypeConversions._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

final case class PartyManagementServiceImpl(invoker: PartyManagementInvoker, api: PartyApi)(implicit
  partyManagementApiKeyValue: PartyManagementApiKeyValue
) extends PartyManagementService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getRelationships(selfcareId: String, personId: UUID, productRoles: Seq[String])(implicit
    contexts: Seq[(String, String)],
    ec: ExecutionContext
  ): Future[Relationships] = withUid[Relationships](uid =>
    selfcareId.toFutureUUID.flatMap { selfcareUUID =>
      val request = api.getRelationships(
        from = Some(personId),
        to = Some(selfcareUUID),
        roles = Seq.empty,
        states = Seq.empty,
        products = Seq(ApplicationConfiguration.selfcareProductId),
        productRoles = productRoles
      )(uid)
      invoker.invoke(request, "Retrieve Relationships")
    }
  )

  override def getRelationshipsByPersonId(personId: UUID, productRoles: Seq[String])(implicit
    contexts: Seq[(String, String)],
    ec: ExecutionContext
  ): Future[Relationships] = withUid[Relationships] { uid =>
    val request = api.getRelationships(
      from = Some(personId),
      to = None,
      roles = Seq.empty,
      states = Seq.empty,
      products = Seq(ApplicationConfiguration.selfcareProductId),
      productRoles = productRoles
    )(uid)
    invoker.invoke(request, "Retrieve Relationships By Person Id")
  }

  override def getRelationshipById(relationshipId: UUID)(implicit
    contexts: Seq[(String, String)],
    ec: ExecutionContext
  ): Future[Relationship] = withUid[Relationship] { uid =>
    val request = api.getRelationshipById(relationshipId)(uid)
    invoker.invoke(request, "Retrieve Relationship By Id")
  }

}
