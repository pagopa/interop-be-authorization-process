package it.pagopa.interop.authorizationprocess.common.system

import com.typesafe.config.{Config, ConfigFactory}
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig
object ApplicationConfiguration {
  val config: Config = ConfigFactory.load()

  val serverPort: Int = config.getInt("authorization-process.port")

  val getAuthorizationManagementURL: String =
    config.getString("authorization-process.services.authorization-management")

  val selfcareV2ClientURL: String    = config.getString("authorization-process.services.selfcare-v2-client")
  val selfcareV2ClientApiKey: String = config.getString("authorization-process.api-keys.selfcare-v2-client")

  val getInteropIdIssuer: String = config.getString("authorization-process.issuer")

  val jwtAudience: Set[String] =
    config.getString("authorization-process.jwt.audience").split(",").toSet.filter(_.nonEmpty)

  require(jwtAudience.nonEmpty, "Audience cannot be empty")

  val readModelConfig: ReadModelConfig = {
    val connectionString: String = config.getString("authorization-process.read-model.db.connection-string")
    val dbName: String           = config.getString("authorization-process.read-model.db.name")

    ReadModelConfig(connectionString, dbName)
  }
}
