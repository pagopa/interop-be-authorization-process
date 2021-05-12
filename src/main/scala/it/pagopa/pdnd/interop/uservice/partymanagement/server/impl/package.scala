package it.pagopa.pdnd.interop.uservice.partymanagement.server

import com.bettercloud.vault.{SslConfig, Vault, VaultConfig}

package object impl {

  def getVaultClient: Vault = {
    val vaultAddress = System.getenv("VAULT_ADDR")
    val vaultToken   = System.getenv("VAULT_TOKEN")
    val config = new VaultConfig()
      .address(vaultAddress)
      .token(vaultToken)
      .sslConfig(new SslConfig().verify(false).build())
      .build()
    new Vault(config)
  }

}
