package it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl

import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{PurposeManagementInvoker, PurposeManagementService}
import it.pagopa.pdnd.interop.uservice.purposemanagement.client.api.PurposeApi
import it.pagopa.pdnd.interop.uservice.purposemanagement.client.invoker.{ApiRequest, BearerToken}
import it.pagopa.pdnd.interop.uservice.purposemanagement.client.model.Purpose
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.Future

final case class PurposeManagementServiceImpl(invoker: PurposeManagementInvoker, api: PurposeApi)
    extends PurposeManagementService {

  implicit val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def getPurpose(bearerToken: String)(purposeId: UUID): Future[Purpose] = {
    val request: ApiRequest[Purpose] = api.getPurpose(purposeId)(BearerToken(bearerToken))
    invoker.invoke(request, "Retrieving Purpose")
  }
}
