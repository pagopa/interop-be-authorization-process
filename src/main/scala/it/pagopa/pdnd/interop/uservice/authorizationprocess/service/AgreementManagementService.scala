package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.model.Agreement

import scala.concurrent.Future

trait AgreementManagementService {

  /** Returns the expected audience defined by the producer of the corresponding agreementId.
    *
    * @param agreementId
    * @return
    */

  def getAgreements(bearerToken: String, consumerId: String, eserviceId: String): Future[Seq[Agreement]]
}
