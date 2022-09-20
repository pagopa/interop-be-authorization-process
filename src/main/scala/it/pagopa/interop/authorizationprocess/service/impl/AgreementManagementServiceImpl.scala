package it.pagopa.interop.authorizationprocess.service.impl

import it.pagopa.interop.agreementmanagement.client.api.AgreementApi
import it.pagopa.interop.agreementmanagement.client.invoker.BearerToken
import it.pagopa.interop.agreementmanagement.client.model.Agreement
import it.pagopa.interop.authorizationprocess.service.{AgreementManagementInvoker, AgreementManagementService}
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}

import java.util.UUID
import scala.concurrent.Future
import it.pagopa.interop.agreementmanagement.client.invoker.ApiRequest

final case class AgreementManagementServiceImpl(invoker: AgreementManagementInvoker, api: AgreementApi)
    extends AgreementManagementService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getAgreements(eServiceId: UUID, consumerId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[Seq[Agreement]] = withHeaders[Seq[Agreement]] { (bearerToken, correlationId, ip) =>
    val request: ApiRequest[Seq[Agreement]] = api.getAgreements(
      xCorrelationId = correlationId,
      xForwardedFor = ip,
      eserviceId = Some(eServiceId.toString),
      consumerId = Some(consumerId.toString),
      states = List.empty
    )(BearerToken(bearerToken))
    invoker.invoke[Seq[Agreement]](request, "Retrieving Agreements")
  }

}
