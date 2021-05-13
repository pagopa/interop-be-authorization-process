package it.pagopa.pdnd.interop.uservice.partymanagement.service

import com.auth0.jwt.algorithms.Algorithm
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.{ErrorOr, PemUtils}

import java.security.interfaces.{ECPrivateKey, ECPublicKey, RSAPrivateKey, RSAPublicKey}
import scala.util.Try
package object impl {
  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  private final val NULL: Null = null

  def generateAlgorithm(algorithm: String, key: String): ErrorOr[Algorithm] = {

    val rsaPublicKey: ErrorOr[RSAPublicKey] =
      PemUtils.readPublicKeyFromString(key, "RSA").map(_.asInstanceOf[RSAPublicKey])

    val rsaPrivateKey: ErrorOr[RSAPrivateKey] =
      PemUtils.readPrivateKeyFromString(key, "RSA").map(_.asInstanceOf[RSAPrivateKey])

    val ecPublicKey: ErrorOr[ECPublicKey] =
      PemUtils.readPublicKeyFromString(key, "EC").map(_.asInstanceOf[ECPublicKey])

    val ecPrivateKey: ErrorOr[ECPrivateKey] =
      PemUtils.readPrivateKeyFromString(key, "EC").map(_.asInstanceOf[ECPrivateKey])

    algorithm match {
      case "HS256"  => getHSAlgorithm(key, Algorithm.HMAC256)
      case "HS384"  => getHSAlgorithm(key, Algorithm.HMAC384)
      case "HS512"  => getHSAlgorithm(key, Algorithm.HMAC512)
      case "RS256"  => getRSAAlgorithm(rsaPublicKey, rsaPrivateKey, Algorithm.RSA256)
      case "RS384"  => getRSAAlgorithm(rsaPublicKey, rsaPrivateKey, Algorithm.RSA384)
      case "RS512"  => getRSAAlgorithm(rsaPublicKey, rsaPrivateKey, Algorithm.RSA512)
      case "ES256"  => getESAlgorithm(ecPublicKey, ecPrivateKey, Algorithm.ECDSA256)
      case "ES256K" => getESAlgorithm(ecPublicKey, ecPrivateKey, Algorithm.ECDSA256K)
      case "ES384"  => getESAlgorithm(ecPublicKey, ecPrivateKey, Algorithm.ECDSA384)
      case "ES512"  => getESAlgorithm(ecPublicKey, ecPrivateKey, Algorithm.ECDSA512)

    }
  }

  private def getHSAlgorithm(secret: String, hsAlgorithmFunc: String => Algorithm): ErrorOr[Algorithm] =
    Try(hsAlgorithmFunc(secret)).toEither

  private def getRSAAlgorithm(
    rsaPublicKey: ErrorOr[RSAPublicKey],
    rsaPrivateKey: ErrorOr[RSAPrivateKey],
    rsaAlgorithmFunc: (RSAPublicKey, RSAPrivateKey) => Algorithm
  ): ErrorOr[Algorithm] =
    Try(rsaAlgorithmFunc(rsaPublicKey.getOrElse(NULL), rsaPrivateKey.getOrElse(NULL))).toEither

  private def getESAlgorithm(
    ecPublicKey: ErrorOr[ECPublicKey],
    ecPrivateKey: ErrorOr[ECPrivateKey],
    esAlgorithmFunc: (ECPublicKey, ECPrivateKey) => Algorithm
  ): ErrorOr[Algorithm] =
    Try(esAlgorithmFunc(ecPublicKey.getOrElse(NULL), ecPrivateKey.getOrElse(NULL))).toEither

}
