package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import it.pagopa.pdnd.interop.uservice.partymanagement.client.model.Organization

import java.util.UUID
import scala.concurrent.Future

trait PartyManagementService {

  def getOrganization(organizationId: UUID): Future[Organization]
}
