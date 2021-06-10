package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

trait VaultService {
  def getSecret(path: String): Map[String, String]
  def getKeysList(path: String): List[String]
}
