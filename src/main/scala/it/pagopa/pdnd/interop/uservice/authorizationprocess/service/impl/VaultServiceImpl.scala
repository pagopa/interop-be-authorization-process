//package it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl
//
//import com.bettercloud.vault.Vault
//import com.bettercloud.vault.response.LogicalResponse
//import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.VaultService
//
//import java.util.Base64
//import scala.jdk.CollectionConverters.MapHasAsScala
//
//final case class VaultServiceImpl(vault: Vault) extends VaultService {
//
//  override def getSecret(path: String): Map[String, String] = {
//    val data: LogicalResponse = vault.logical().read(path)
//
//    data.getData.asScala.toMap.view
//      .mapValues(decodeBase64)
//      .toMap
//  }
//
//  private def decodeBase64(encoded: String): String = {
//    val decoded: Array[Byte] = Base64.getDecoder.decode(encoded)
//    new String(decoded)
//  }
//
//}
