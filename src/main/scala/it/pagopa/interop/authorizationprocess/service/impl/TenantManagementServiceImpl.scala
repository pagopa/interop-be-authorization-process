package it.pagopa.interop.authorizationprocess.service.impl

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.authorizationprocess.service.TenantManagementService
import it.pagopa.interop.tenantmanagement.client.invoker.{ApiError, ApiInvoker, ApiRequest, BearerToken}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.tenantmanagement.client.api.{EnumsSerializers, TenantApi}
import it.pagopa.interop.tenantmanagement.client.model.Tenant

import java.util.UUID
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import akka.actor.typed.ActorSystem
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors.TenantNotFound
import it.pagopa.interop.commons.utils.withHeaders

class TenantManagementServiceImpl(tenantManagementUrl: String, blockingEc: ExecutionContextExecutor)(implicit
  system: ActorSystem[_],
  ec: ExecutionContext
) extends TenantManagementService {

  val invoker: ApiInvoker = ApiInvoker(EnumsSerializers.all, blockingEc)(system.classicSystem)
  val api: TenantApi      = TenantApi(tenantManagementUrl)

  private implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getTenant(tenantId: UUID)(implicit contexts: Seq[(String, String)]): Future[Tenant] =
    withHeaders[Tenant] { (bearerToken, correlationId, ip) =>
      val request: ApiRequest[Tenant] =
        api.getTenant(xCorrelationId = correlationId, tenantId = tenantId, xForwardedFor = ip)(BearerToken(bearerToken))
      invoker
        .invoke(request, s"Retrieving Tenant $tenantId")
        .recoverWith {
          case err: ApiError[_] if err.code == 404 => Future.failed(TenantNotFound(tenantId))
        }
    }

}
