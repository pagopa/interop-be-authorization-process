package it.pagopa.pdnd.interop.uservice.partymanagement.service

trait VaultService {
  def getSecret(path: String): Map[String, String]
}
