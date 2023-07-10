package it.pagopa.interop.authorizationprocess.service.impl

import it.pagopa.interop.authorizationprocess.service.TenantManagementService
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors.TenantNotFound
import it.pagopa.interop.authorizationprocess.common.readmodel.ReadModelTenantQueries
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.commons.utils.TypeConversions._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

object TenantManagementServiceImpl extends TenantManagementService {
  override def getTenantById(
    tenantId: UUID
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PersistentTenant] = {
    ReadModelTenantQueries.getTenantById(tenantId).flatMap(_.toFuture(TenantNotFound(tenantId)))
  }
}
