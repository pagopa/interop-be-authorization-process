package it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl

import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.api.AgreementApi
import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.invoker.{ApiRequest, BearerToken}
import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.model.Agreement
import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.model.AgreementEnums.Status
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{
  AgreementManagementInvoker,
  AgreementManagementService
}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class AgreementManagementServiceImpl(invoker: AgreementManagementInvoker, api: AgreementApi)(implicit
  ec: ExecutionContext
) extends AgreementManagementService {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def getAgreements(
    bearerToken: String,
    consumerId: String,
    eserviceId: String,
    status: Status
  ): Future[Seq[Agreement]] = {
    val request: ApiRequest[Seq[Agreement]] =
      api.getAgreements(consumerId = Some(consumerId), eserviceId = Some(eserviceId), status = Some(status.toString))(
        BearerToken(bearerToken)
      )
    invoker
      .execute[Seq[Agreement]](request)
      .map { x =>
        logger.info(s"Retrieving active agreements for $consumerId status code > ${x.code.toString}")
        logger.info(s"Retrieving active agreements for $consumerId content > ${x.content.toString}")
        x.content
      }
      .recoverWith { case ex =>
        logger.error(s"Retrieving active agreements for $consumerId, error > ${ex.getMessage}")
        Future.failed[Seq[Agreement]](ex)
      }
  }
}
