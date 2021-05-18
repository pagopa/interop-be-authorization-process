package it.pagopa.pdnd.interop.uservice.partymanagement.service.impl

import com.bettercloud.vault.Vault
import com.bettercloud.vault.response.LogicalResponse
import it.pagopa.pdnd.interop.uservice.partymanagement.service.VaultService

import scala.jdk.CollectionConverters.MapHasAsScala

class VaultServiceImpl(vault: Vault) extends VaultService {
  override def getSecret(path: String): Map[String, String] = {
    val data: LogicalResponse = vault.logical().read(path)
    data.getData.asScala.toMap
  }
}
