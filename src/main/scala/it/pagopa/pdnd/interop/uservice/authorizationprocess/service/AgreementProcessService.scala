package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import it.pagopa.pdnd.interop.uservice.agreementprocess.client.model.Audience

import scala.concurrent.Future

trait AgreementProcessService {

  /** Returns the expected audience defined by the producer of the corresponding agreementId.
    *
    * @param agreementId
    * @return
    */

  def retrieveAudience(bearerToken: String, agreementId: String): Future[Audience]
}
