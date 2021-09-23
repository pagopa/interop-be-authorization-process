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
  def getPersonByTaxCode(taxCode: String): Future[Person]
  def getRelationships(institutionId: String, personTaxCode: String, platformRole: String): Future[Relationships]
  def getRelationshipsByTaxCode(personTaxCode: String, platformRole: String): Future[Relationships]
  def getRelationshipById(relationshipId: UUID): Future[Relationship]
}

object PartyManagementService {

  final val ROLE_SECURITY_OPERATOR = "security"

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
