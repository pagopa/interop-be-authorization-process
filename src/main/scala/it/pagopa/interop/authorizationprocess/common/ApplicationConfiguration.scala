package it.pagopa.interop.authorizationprocess.common

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationConfiguration {
  val config: Config = ConfigFactory.load()

  val serverPort: Int = config.getInt("authorization-process.port")

  val getAuthorizationManagementURL: String =
    config.getString("authorization-process.services.authorization-management")
  val getAgreementManagementURL: String     = config.getString("authorization-process.services.agreement-management")
  val getCatalogManagementURL: String       = config.getString("authorization-process.services.catalog-management")
  val getPartyManagementURL: String         = config.getString("authorization-process.services.party-management")
  val getPurposeManagementURL: String       = config.getString("authorization-process.services.purpose-management")
  val getUserRegistryManagementURL: String  =
    config.getString("authorization-process.services.user-registry-management")

  val userRegistryApiKey: String    = config.getString("authorization-process.api-keys.user-registry-api-key")
  val partyManagementApiKey: String = config.getString("authorization-process.api-keys.party-management")

  val getInteropIdIssuer: String = config.getString("authorization-process.issuer")

  val jwtAudience: Set[String] =
    config.getString("authorization-process.jwt.audience").split(",").toSet.filter(_.nonEmpty)

  require(jwtAudience.nonEmpty, "Audience cannot be empty")

}
