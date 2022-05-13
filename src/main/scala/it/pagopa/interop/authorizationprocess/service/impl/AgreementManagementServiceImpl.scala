package it.pagopa.interop.authorizationprocess.service.impl

import it.pagopa.interop.agreementmanagement.client.api.AgreementApi
import it.pagopa.interop.agreementmanagement.client.invoker.BearerToken
import it.pagopa.interop.agreementmanagement.client.model.Agreement
import it.pagopa.interop.authorizationprocess.service.{AgreementManagementInvoker, AgreementManagementService}
import it.pagopa.interop.commons.utils.TypeConversions.EitherOps
import it.pagopa.interop.commons.utils.extractHeaders
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

final case class AgreementManagementServiceImpl(invoker: AgreementManagementInvoker, api: AgreementApi)(implicit
  ec: ExecutionContext
) extends AgreementManagementService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getAgreements(eServiceId: UUID, consumerId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[Seq[Agreement]] = {
    for {
      (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
      request = api.getAgreements(
        xCorrelationId = correlationId,
        xForwardedFor = ip,
        eserviceId = Some(eServiceId.toString),
        consumerId = Some(consumerId.toString)
      )(BearerToken(bearerToken))
      result <- invoker.invoke[Seq[Agreement]](request, "Retrieving Agreements")
    } yield result
  }
}
