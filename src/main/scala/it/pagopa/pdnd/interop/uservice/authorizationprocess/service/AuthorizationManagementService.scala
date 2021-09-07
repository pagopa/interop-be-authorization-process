package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.Client

import java.util.UUID
import scala.concurrent.Future

trait AuthorizationManagementService {

  /** Returns the expected audience defined by the producer of the corresponding agreementId.
    *
    * @param agreementId
    * @return
    */

  def createClient(agreementId: UUID, description: String): Future[Client]
}
