package it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl

import it.pagopa.pdnd.interop.uservice.agreementprocess.client.api.ProcessApi
import it.pagopa.pdnd.interop.uservice.agreementprocess.client.invoker.ApiRequest
import it.pagopa.pdnd.interop.uservice.agreementprocess.client.model.Audience
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{AgreementProcessInvoker, AgreementProcessService}
import it.pagopa.pdnd.interop.uservice.agreementprocess.client.invoker.BearerToken
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class AgreementProcessServiceImpl(invoker: AgreementProcessInvoker, api: ProcessApi)(implicit ec: ExecutionContext)
    extends AgreementProcessService {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /** Returns the expected audience defined by the producer of the corresponding agreementId.
    *
    * @param agreementId
    * @return
    */
  override def retrieveAudience(bearerToken: String, agreementId: String): Future[Audience] = {
    val request: ApiRequest[Audience] = api.getAudienceByAgreementId(agreementId)(BearerToken(bearerToken))
    invoker
      .execute[Audience](request)
      .map { x =>
        logger.info(s"Retrieving audience status code > ${x.code.toString}")
        logger.info(s"Retrieving audience content > ${x.content.toString}")
        x.content
      }
      .recoverWith { case ex =>
        logger.error(s"Retrieving audience, error > ${ex.getMessage}")
        Future.failed[Audience](ex)
      }
  }
}
