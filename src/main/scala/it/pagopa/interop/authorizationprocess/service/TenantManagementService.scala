package it.pagopa.interop.authorizationprocess.service
import it.pagopa.interop.tenantmanagement.client.model.Tenant
import it.pagopa.interop.authorizationprocess.model.{Organization => ApiOrganization}

import java.util.UUID
import scala.concurrent.Future

object TenantManagementService {
  def tenantToApi(tenant: Tenant): ApiOrganization = ApiOrganization(tenant.externalId.value, tenant.name)
}
trait TenantManagementService  {
  def getTenant(tenantId: UUID)(implicit contexts: Seq[(String, String)]): Future[Tenant]
}
