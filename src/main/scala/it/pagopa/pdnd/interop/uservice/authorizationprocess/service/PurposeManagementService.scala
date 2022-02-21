package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import it.pagopa.pdnd.interop.uservice.purposemanagement.client.model.Purpose

import java.util.UUID
import scala.concurrent.Future

trait PurposeManagementService {
  def getPurpose(bearerToken: String)(purposeId: UUID): Future[Purpose]
}
