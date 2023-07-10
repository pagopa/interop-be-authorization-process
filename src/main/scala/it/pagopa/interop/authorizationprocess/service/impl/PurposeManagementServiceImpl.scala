package it.pagopa.interop.authorizationprocess.service.impl

import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors.PurposeNotFound
import it.pagopa.interop.authorizationprocess.service.PurposeManagementService
import it.pagopa.interop.purposemanagement.model.purpose.PersistentPurpose
import it.pagopa.interop.authorizationprocess.common.readmodel.ReadModelPurposeQueries
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.commons.utils.TypeConversions._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

final object PurposeManagementServiceImpl extends PurposeManagementService {

  override def getPurposeById(
    purposeId: UUID
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PersistentPurpose] =
    ReadModelPurposeQueries.getPurposeById(purposeId).flatMap(_.toFuture(PurposeNotFound(purposeId)))
}
