package it.pagopa.pdnd.interop.uservice.partymanagement.common.utils

import org.bouncycastle.util.io.pem.{PemObject, PemReader}

import java.io.{ByteArrayInputStream, InputStreamReader}
import java.nio.charset.StandardCharsets
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, PrivateKey, PublicKey}
import scala.util.Try

object PemUtils {

  def readPrivateKeyFromString(keyString: String, algorithm: String): Try[PrivateKey] =
    parsePEMString(keyString).flatMap(PemUtils.getPrivateKey(algorithm))

  def readPublicKeyFromString(keyString: String, algorithm: String): Try[PublicKey] =
    parsePEMString(keyString).flatMap(PemUtils.getPublicKey(algorithm))

  private def parsePEMString(pem: String): Try[Array[Byte]] = Try {
    val byteStream: ByteArrayInputStream = new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8.name))
    val inputStream: InputStreamReader   = new InputStreamReader(byteStream)
    val reader: PemReader                = new PemReader(inputStream)
    val pemObject: PemObject             = reader.readPemObject
    val content                          = pemObject.getContent
    reader.close()
    content
  }

  private def getPrivateKey(algorithm: String): Array[Byte] => Try[PrivateKey] = keyBytes => {
    Try {
      val kf: KeyFactory               = KeyFactory.getInstance(algorithm)
      val keySpec: PKCS8EncodedKeySpec = new PKCS8EncodedKeySpec(keyBytes)
      kf.generatePrivate(keySpec)
    }
  }

  private def getPublicKey(algorithm: String): Array[Byte] => Try[PublicKey] = keyBytes => {
    Try {
      val kf: KeyFactory              = KeyFactory.getInstance(algorithm)
      val keySpec: X509EncodedKeySpec = new X509EncodedKeySpec(keyBytes)
      kf.generatePublic(keySpec)
    }
  }

}
