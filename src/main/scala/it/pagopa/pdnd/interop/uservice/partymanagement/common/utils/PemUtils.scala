package it.pagopa.pdnd.interop.uservice.partymanagement.common.utils

import org.bouncycastle.util.io.pem.{PemObject, PemReader}

import java.io.{ByteArrayInputStream, InputStreamReader}
import java.nio.charset.StandardCharsets
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, PrivateKey, PublicKey}
import scala.util.Try

object PemUtils {

  def readPrivateKeyFromString(keyString: String, algorithm: String): ErrorOr[PrivateKey] = {
    val bytes: Array[Byte] = parsePEMString(keyString)

    PemUtils.getPrivateKey(bytes, algorithm)
  }

  def readPublicKeyFromString(keyString: String, algorithm: String): ErrorOr[PublicKey] = {
    val bytes: Array[Byte] = parsePEMString(keyString)
    PemUtils.getPublicKey(bytes, algorithm)
  }

  private def parsePEMString(pem: String): Array[Byte] = {
    val byteStream: ByteArrayInputStream = new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8.name))
    val inputStream: InputStreamReader   = new InputStreamReader(byteStream)
    val reader: PemReader                = new PemReader(inputStream)
    val pemObject: PemObject             = reader.readPemObject
    val content                          = pemObject.getContent
    reader.close()
    content
  }

  private def getPrivateKey(keyBytes: Array[Byte], algorithm: String): ErrorOr[PrivateKey] = {
    Try {
      val kf: KeyFactory               = KeyFactory.getInstance(algorithm)
      val keySpec: PKCS8EncodedKeySpec = new PKCS8EncodedKeySpec(keyBytes)
      val privateKey: PrivateKey       = kf.generatePrivate(keySpec)
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
