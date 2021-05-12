package it.pagopa.pdnd.interop.uservice.partymanagement.common.utils

import org.slf4j.{Logger, LoggerFactory}

import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, PrivateKey, PublicKey}
import scala.util.Try

object PemUtils {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def readPrivateKeyFromString(keyString: String, algorithm: String): ErrorOr[PrivateKey] =
    PemUtils.getPrivateKey(keyString.getBytes, algorithm)

  def readPublicKeyFromString(keyString: String, algorithm: String): ErrorOr[PublicKey] =
    PemUtils.getPublicKey(keyString.getBytes, algorithm)

  private def getPrivateKey(keyBytes: Array[Byte], algorithm: String): ErrorOr[PrivateKey] = {
    Try {
      logger.info(algorithm)
      val kf: KeyFactory               = KeyFactory.getInstance("RSA")
      val keySpec: PKCS8EncodedKeySpec = new PKCS8EncodedKeySpec(keyBytes)
      val privateKey: PrivateKey       = kf.generatePrivate(keySpec)
      privateKey
    }.toEither
  }

  private def getPublicKey(keyBytes: Array[Byte], algorithm: String): ErrorOr[PublicKey] = {
    Try {
      logger.info(new String(keyBytes))
      val kf: KeyFactory              = KeyFactory.getInstance("RSA")
      val keySpec: X509EncodedKeySpec = new X509EncodedKeySpec(keyBytes)
      logger.info(algorithm)
      logger.info(keySpec.toString)
      logger.info(keySpec.getFormat)
      logger.info(keySpec.getAlgorithm)
      val publicKey: PublicKey = kf.generatePublic(keySpec)
      logger.info(publicKey.toString)
      publicKey
    }.toEither
  }

}
