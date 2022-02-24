package it.pagopa.interop.authorizationprocess.service.impl

import it.pagopa.interop.agreementmanagement.client.api.AgreementApi
import it.pagopa.interop.agreementmanagement.client.invoker.{ApiRequest, BearerToken}
import it.pagopa.interop.agreementmanagement.client.model.Agreement
import it.pagopa.interop.authorizationprocess.service.{AgreementManagementInvoker, AgreementManagementService}
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.Future

final case class AgreementManagementServiceImpl(invoker: AgreementManagementInvoker, api: AgreementApi)
    extends AgreementManagementService {

  implicit val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def getAgreements(bearerToken: String)(eServiceId: UUID, consumerId: UUID): Future[Seq[Agreement]] = {
    val request: ApiRequest[Seq[Agreement]] =
      api.getAgreements(eserviceId = Some(eServiceId.toString), consumerId = Some(consumerId.toString))(
        BearerToken(bearerToken)
      )
    invoker.invoke[Seq[Agreement]](request, "Retrieving Agreements")
  }
}
