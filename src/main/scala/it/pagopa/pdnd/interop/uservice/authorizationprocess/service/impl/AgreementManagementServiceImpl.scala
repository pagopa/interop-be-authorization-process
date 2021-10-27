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

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class AgreementManagementServiceImpl(invoker: AgreementManagementInvoker, api: AgreementApi)(implicit
  ec: ExecutionContext
) extends AgreementManagementService {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def getAgreements(
    bearerToken: String,
    consumerId: UUID,
    eserviceId: UUID,
    status: Option[Status]
  ): Future[Seq[Agreement]] = {
    val request: ApiRequest[Seq[Agreement]] =
      api.getAgreements(
        consumerId = Some(consumerId.toString),
        eserviceId = Some(eserviceId.toString),
        status = status.map(_.toString)
      )(BearerToken(bearerToken))
    invoker
      .execute[Seq[Agreement]](request)
      .map { x =>
        logger.info(s"Retrieving active agreements for ${consumerId.toString} status code > ${x.code.toString}")
        logger.info(s"Retrieving active agreements for ${consumerId.toString} content > ${x.content.toString}")
        x.content
      }
      .recoverWith { case ex =>
        logger.error(s"Retrieving active agreements for ${consumerId.toString}, error > ${ex.getMessage}")
        Future.failed[Seq[Agreement]](ex)
      }
  }
}
