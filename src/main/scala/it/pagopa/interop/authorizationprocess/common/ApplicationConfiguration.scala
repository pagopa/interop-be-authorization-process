package it.pagopa.interop.authorizationprocess.common

import com.typesafe.config.{Config, ConfigFactory}
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig
object ApplicationConfiguration {
  val config: Config = ConfigFactory.load()

  val serverPort: Int = config.getInt("authorization-process.port")

  val getAuthorizationManagementURL: String =
    config.getString("authorization-process.services.authorization-management")
  val getAgreementManagementURL: String     = config.getString("authorization-process.services.agreement-management")
  val getCatalogManagementURL: String       = config.getString("authorization-process.services.catalog-management")
  val getPartyManagementURL: String         = config.getString("authorization-process.services.party-management")
  val getPurposeManagementURL: String       = config.getString("authorization-process.services.purpose-management")
  val getTenantManagementURL: String        = config.getString("authorization-process.services.tenant-management")
  val getUserRegistryManagementURL: String  =
    config.getString("authorization-process.services.user-registry-management")

  val userRegistryApiKey: String    = config.getString("authorization-process.api-keys.user-registry")
  val partyManagementApiKey: String = config.getString("authorization-process.api-keys.party-management")

  val getInteropIdIssuer: String = config.getString("authorization-process.issuer")

  val jwtAudience: Set[String] =
    config.getString("authorization-process.jwt.audience").split(",").toSet.filter(_.nonEmpty)

  require(jwtAudience.nonEmpty, "Audience cannot be empty")

  val selfcareProductId: String = config.getString("authorization-process.selfcare-product-id")

  val readModelConfig: ReadModelConfig = {
    val connectionString: String = config.getString("authorization-process.read-model.db.connection-string")
    val dbName: String           = config.getString("authorization-process.read-model.db.name")

    ReadModelConfig(connectionString, dbName)
  }
}
