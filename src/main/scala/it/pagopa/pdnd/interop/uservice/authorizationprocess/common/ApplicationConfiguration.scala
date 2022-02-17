package it.pagopa.pdnd.interop.uservice.authorizationprocess.common

import com.typesafe.config.{Config, ConfigFactory}

import scala.jdk.CollectionConverters.ListHasAsScala

object ApplicationConfiguration {
  lazy val config: Config = ConfigFactory.load()

  lazy val serverPort: Int = config.getInt("authorization-process.port")

  lazy val getAuthorizationManagementURL: String =
    config.getString("authorization-process.services.authorization-management")

  lazy val getPartyManagementURL: String = config.getString("authorization-process.services.party-management")

  lazy val getUserRegistryManagementURL: String =
    config.getString("authorization-process.services.user-registry-management")

  lazy val userRegistryApiKey: String = config.getString("authorization-process.services.user-registry-api-key")

  lazy val getPdndIdIssuer: String = config.getString("authorization-process.issuer")

  lazy val getVaultSecretsRootPath: String = config.getString("authorization-process.vault-root-path")

  lazy val jwtAudience: Set[String] = config.getStringList("authorization-process.jwt.audience").asScala.toSet
}
