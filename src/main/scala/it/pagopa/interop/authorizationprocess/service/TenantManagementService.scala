package it.pagopa.interop.authorizationprocess.service

import it.pagopa.interop.authorizationprocess.model.{Organization => ApiOrganization}
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant
import it.pagopa.interop.commons.cqrs.service.ReadModelService

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}

trait TenantManagementService {
  def getTenantById(
    tenantId: UUID
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PersistentTenant]
}

object TenantManagementService {
  def tenantToApi(tenant: PersistentTenant): ApiOrganization = ApiOrganization(tenant.externalId.value, tenant.name)
}
