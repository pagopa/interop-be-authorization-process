package it.pagopa.pdnd.interop.uservice.partymanagement.common.utils

import java.security.spec.X509EncodedKeySpec
import java.security.{KeyFactory, PrivateKey, PublicKey}
import scala.util.Try

object PemUtils {

  def readPrivateKeyFromString(keyString: Option[String], algorithm: String): ErrorOr[PrivateKey] =
    keyString match {
      case Some(key) => PemUtils.getPrivateKey(key.getBytes, algorithm)
      case None      => Left(new RuntimeException(s"Key not found for $algorithm algorithm"))
    }

  def readPublicKeyFromString(keyString: Option[String], algorithm: String): ErrorOr[PublicKey] =
    keyString match {
      case Some(key) => PemUtils.getPublicKey(key.getBytes, algorithm)
      case None      => Left(new RuntimeException(s"Key not found for $algorithm algorithm"))
    }

  private def getPrivateKey(keyBytes: Array[Byte], algorithm: String): ErrorOr[PrivateKey] = {
    Try {
      val kf: KeyFactory              = KeyFactory.getInstance(algorithm)
      val keySpec: X509EncodedKeySpec = new X509EncodedKeySpec(keyBytes)
      val privateKey: PrivateKey      = kf.generatePrivate(keySpec)
      privateKey
    }.toEither
  }

  private def getPublicKey(keyBytes: Array[Byte], algorithm: String): ErrorOr[PublicKey] = {
    Try {
      val kf: KeyFactory              = KeyFactory.getInstance(algorithm)
      val keySpec: X509EncodedKeySpec = new X509EncodedKeySpec(keyBytes)
      val publicKey: PublicKey        = kf.generatePublic(keySpec)
      publicKey
    }.toEither
  }

}
