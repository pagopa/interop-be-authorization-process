package it.pagopa.pdnd.interop.uservice.authorizationprocess.common

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationConfiguration {
  lazy val config: Config = ConfigFactory.load()

  def serverPort: Int = {
    config.getInt("uservice-authorization-process.port")
  }

  def getKeyManagementUrl: String = {
    val keyManagementUrl: String = config.getString("services.key-management")
    s"$keyManagementUrl/pdnd-interop-uservice-key-management/0.0.1"
  }

  def getAgreementManagementURL: String = config.getString("services.agreement-management")

  def getCatalogManagementURL: String = config.getString("services.catalog-management")

  def getAuthorizationManagementURL: String = config.getString("services.key-management")

  def getPartyManagementURL: String = config.getString("services.party-management")

  def getPdndIdIssuer: String = {
    config.getString("uservice-authorization-process.issuer")
  }

}
