package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.{
  Operator => ApiOperator,
  Organization => ApiOrganization
}
import it.pagopa.pdnd.interop.uservice.partymanagement.client.model.{Organization, Person, Relationship, Relationships}

import java.util.UUID
import scala.concurrent.Future

trait PartyManagementService {

  def getOrganization(organizationId: UUID): Future[Organization]
  def getPerson(personId: UUID): Future[Person]
  def getRelationships(organizationId: String, personId: String): Future[Relationships]
}

object PartyManagementService {

  def organizationToApi(organization: Organization): ApiOrganization =
    ApiOrganization(organization.institutionId, organization.description)

  def operatorToApi(person: Person, relationship: Relationship): ApiOperator =
    ApiOperator(
      taxCode = person.taxCode,
      name = person.name,
      surname = person.surname,
      role = relationship.role.toString,
      platformRole = relationship.platformRole
    )

}
