package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import com.auth0.jwt.algorithms.Algorithm
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.utils.PemUtils

import java.security.interfaces.{ECPrivateKey, ECPublicKey, RSAPrivateKey, RSAPublicKey}
import scala.util.{Failure, Try}
package object impl {
  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  private final val NULL: Null = null

  def generateAlgorithm(algorithm: String, key: String): Try[Algorithm] = {

    val rsaPublicKey: Try[RSAPublicKey] =
      PemUtils.readPublicKeyFromString(key, "RSA").map(_.asInstanceOf[RSAPublicKey])

    val rsaPrivateKey: Try[RSAPrivateKey] =
      PemUtils.readPrivateKeyFromString(key, "RSA").map(_.asInstanceOf[RSAPrivateKey])

    val ecPublicKey: Try[ECPublicKey] =
      PemUtils.readPublicKeyFromString(key, "EC").map(_.asInstanceOf[ECPublicKey])

    val ecPrivateKey: Try[ECPrivateKey] =
      PemUtils.readPrivateKeyFromString(key, "EC").map(_.asInstanceOf[ECPrivateKey])

    algorithm match {
//      case "HS256"  => getHSAlgorithm(key, Algorithm.HMAC256)
//      case "HS384"  => getHSAlgorithm(key, Algorithm.HMAC384)
//      case "HS512"  => getHSAlgorithm(key, Algorithm.HMAC512)
      case "RS256"  => getRSAAlgorithm(rsaPublicKey, rsaPrivateKey, Algorithm.RSA256)
      case "RS384"  => getRSAAlgorithm(rsaPublicKey, rsaPrivateKey, Algorithm.RSA384)
      case "RS512"  => getRSAAlgorithm(rsaPublicKey, rsaPrivateKey, Algorithm.RSA512)
      case "ES256"  => getESAlgorithm(ecPublicKey, ecPrivateKey, Algorithm.ECDSA256)
      case "ES256K" => getESAlgorithm(ecPublicKey, ecPrivateKey, Algorithm.ECDSA256K)
      case "ES384"  => getESAlgorithm(ecPublicKey, ecPrivateKey, Algorithm.ECDSA384)
      case "ES512"  => getESAlgorithm(ecPublicKey, ecPrivateKey, Algorithm.ECDSA512)
      case _        => Failure(new RuntimeException("sdfhjsdhfjksdh"))
    }
  }

//  private def getHSAlgorithm(secret: String, hsAlgorithmFunc: String => Algorithm): Try[Algorithm] =
//    Try(hsAlgorithmFunc(secret))

  private def getRSAAlgorithm(
    rsaPublicKey: Try[RSAPublicKey],
    rsaPrivateKey: Try[RSAPrivateKey],
    rsaAlgorithmFunc: (RSAPublicKey, RSAPrivateKey) => Algorithm
  ): Try[Algorithm] =
    Try(rsaAlgorithmFunc(rsaPublicKey.getOrElse(NULL), rsaPrivateKey.getOrElse(NULL)))

  private def getESAlgorithm(
    ecPublicKey: Try[ECPublicKey],
    ecPrivateKey: Try[ECPrivateKey],
    esAlgorithmFunc: (ECPublicKey, ECPrivateKey) => Algorithm
  ): Try[Algorithm] =
    Try(esAlgorithmFunc(ecPublicKey.getOrElse(NULL), ecPrivateKey.getOrElse(NULL)))

}
