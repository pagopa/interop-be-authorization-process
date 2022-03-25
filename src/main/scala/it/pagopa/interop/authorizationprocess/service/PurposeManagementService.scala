package it.pagopa.interop.authorizationprocess.service

import it.pagopa.interop.purposemanagement.client.model.Purpose

import java.util.UUID
import scala.concurrent.Future

trait PurposeManagementService {
  def getPurpose(contexts: Seq[(String, String)])(purposeId: UUID): Future[Purpose]
}
