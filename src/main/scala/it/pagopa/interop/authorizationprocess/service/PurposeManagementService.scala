package it.pagopa.interop.authorizationprocess.service

import it.pagopa.interop.purposemanagement.model.purpose.PersistentPurpose
import it.pagopa.interop.commons.cqrs.service.ReadModelService

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}

trait PurposeManagementService {
  def getPurposeById(
    purposeId: UUID
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PersistentPurpose]
}
