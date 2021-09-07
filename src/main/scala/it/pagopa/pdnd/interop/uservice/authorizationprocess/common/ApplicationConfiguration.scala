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

  def getAgreementProcessURL: String = {
    val agreementProcessURL: String = config.getString("services.agreement-process")
    s"$agreementProcessURL/pdnd-interop-uservice-agreement-process/0.0.1"
  }

  def getAgreementManagementURL: String = {
    val agreementManagementURL: String = config.getString("services.agreement-management")
    s"$agreementManagementURL/pdnd-interop-uservice-agreement-management/0.0.1"
  }

  def getAuthorizationManagementURL: String = {
    val authorizationManagementURL: String = config.getString("services.key-management")
    s"$authorizationManagementURL/pdnd-interop-uservice-key-management/0.0.1"
  }

  def getPdndIdIssuer: String = {
    config.getString("uservice-authorization-process.issuer")
  }

}
