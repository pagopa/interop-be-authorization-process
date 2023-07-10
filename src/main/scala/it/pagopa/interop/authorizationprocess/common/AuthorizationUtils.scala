package it.pagopa.interop.authorizationprocess.common

import it.pagopa.interop.commons.utils.AkkaUtils.getOrganizationIdUUID
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors._
import java.util.UUID
import it.pagopa.interop.authorizationmanagement.model.client.PersistentClient

object AuthorizationUtils {

  def assertIsClientConsumer(
    client: PersistentClient
  )(implicit contexts: Seq[(String, String)]): Either[Throwable, Unit] =
    for {
      organizationId <- getOrganizationIdUUID(contexts)
      _              <- Either.cond(
        client.consumerId == organizationId,
        (),
        OrganizationNotAllowedOnClient(client.id.toString, client.consumerId)
      )
    } yield ()

  def assertIsPurposeConsumer(purposeId: UUID, consumerId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Either[Throwable, Unit] =
    for {
      organizationId <- getOrganizationIdUUID(contexts)
      _              <- Either.cond(
        consumerId == organizationId,
        (),
        OrganizationNotAllowedOnPurpose(purposeId.toString, consumerId.toString)
      )
    } yield ()
}
