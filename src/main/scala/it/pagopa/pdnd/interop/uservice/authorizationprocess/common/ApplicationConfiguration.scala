package it.pagopa.pdnd.interop.uservice.authorizationprocess.common

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationConfiguration {
  lazy val config: Config = ConfigFactory.load()

  def serverPort: Int = {
    config.getInt("uservice-authorization-process.port")
  }

  def getKeyManagementURL: String           = config.getString("services.key-management")
  def getAgreementManagementURL: String     = config.getString("services.agreement-management")
  def getCatalogManagementURL: String       = config.getString("services.catalog-management")
  def getAuthorizationManagementURL: String = config.getString("services.key-management")
  def getPartyManagementURL: String         = config.getString("services.party-management")
  def getUserRegistryManagementURL: String  = config.getString("services.user-registry-management")
  def userRegistryApiKey: String            = config.getString("services.user-registry-api-key")

  def getPdndIdIssuer: String = {
    config.getString("uservice-authorization-process.issuer")
  }

  def getVaultSecretsRootPath: String = config.getString("uservice-authorization-process.vault-root-path")

}
