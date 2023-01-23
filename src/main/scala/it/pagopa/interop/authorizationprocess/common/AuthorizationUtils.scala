package it.pagopa.interop.authorizationprocess.common

import cats.implicits._
import it.pagopa.interop.commons.utils.AkkaUtils.getOrganizationIdFutureUUID
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors._
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import it.pagopa.interop.authorizationprocess.service.AuthorizationManagementService

object AuthorizationUtils {

  def assertIsClientConsumer(clientId: UUID)(
    authorizationManagementService: AuthorizationManagementService
  )(implicit contexts: Seq[(String, String)], ec: ExecutionContext): Future[Unit] =
    for {
      organizationId <- getOrganizationIdFutureUUID(contexts)
      _              <- authorizationManagementService
        .getClient(clientId)(contexts)
        .ensureOr(client => OrganizationNotAllowedOnClient(clientId.toString, client.consumerId))(
          _.consumerId == organizationId
        )
    } yield ()

  def assertIsPurposeConsumer(purposeId: UUID, consumerId: UUID)(implicit
    contexts: Seq[(String, String)],
    ec: ExecutionContext
  ): Future[Unit] =
    for {
      organizationId <- getOrganizationIdFutureUUID(contexts)
      _              <- Future
        .failed(OrganizationNotAllowedOnPurpose(purposeId.toString, consumerId.toString))
        .unlessA(consumerId == organizationId)

    } yield ()
}
