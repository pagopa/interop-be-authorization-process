package it.pagopa.interop.authorizationprocess.service

import it.pagopa.interop.selfcare.v2.client.model._
import scala.concurrent.{Future, ExecutionContext}
import java.util.UUID

trait SelfcareV2ClientService {

  def getInstitutionProductUsers(selfcareId: UUID, requesterId: UUID, userId: UUID, productRoles: Seq[String])(implicit
    contexts: Seq[(String, String)],
    ec: ExecutionContext
  ): Future[Seq[UserResource]]

  def getUserById(selfcareId: UUID, userId: UUID)(implicit
    contexts: Seq[(String, String)],
    ec: ExecutionContext
  ): Future[UserResponse]
}

object SelfcareV2ClientService {
  final val PRODUCT_ROLE_SECURITY_OPERATOR = "security"
  final val PRODUCT_ROLE_ADMIN             = "admin"
}
