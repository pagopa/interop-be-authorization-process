package it.pagopa.interop.authorizationprocess.service

import it.pagopa.interop.selfcare.v2.client.model._
import scala.concurrent.{Future, ExecutionContext}
import java.util.UUID

trait SelfcareV2Service {

  def getInstitutionProductUsers(selfcareId: UUID, requesterId: UUID, userId: Option[UUID], roles: Seq[String])(implicit
    contexts: Seq[(String, String)],
    ec: ExecutionContext
  ): Future[Seq[UserResource]]
}
