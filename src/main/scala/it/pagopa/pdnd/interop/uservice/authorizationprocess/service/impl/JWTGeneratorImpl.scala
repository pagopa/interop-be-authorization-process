package it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl

import com.nimbusds.jose.crypto.{ECDSASigner, Ed25519Signer, RSASSASigner}
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.{JOSEObjectType, JWSAlgorithm, JWSHeader, JWSSigner}
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.ApplicationConfiguration
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.utils.decodeBase64
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.token.TokenSeed
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{JWTGenerator, VaultService}
import org.slf4j.{Logger, LoggerFactory}

import java.util.Date
import scala.concurrent.Future
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.util.Try

final case class JWTGeneratorImpl(vaultService: VaultService) extends JWTGenerator {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

//   TODO: Start
//   TODO: this part is static and initialized at the start up
//   TODO - use a def instead of a val, but this approach generate to many calls to the vault
//   TODO - use a refreshing cache, more complex
  private val keyRootPath: Try[String] = Try(System.getenv("PDND_INTEROP_PRIVATE_KEY"))

  private val rsaPrivateKey: Try[String] =
    keyRootPath.flatMap { root =>
      val rsaPath = s"$root/${ApplicationConfiguration.getPdndIdIssuer}"
      getPrivateKeyFromVault(rsaPath, "rsa")
    }

  private val ecPrivateKey: Try[String] =
    keyRootPath.flatMap { root =>
      val rsaPath = s"$root/${ApplicationConfiguration.getPdndIdIssuer}"
      getPrivateKeyFromVault(rsaPath, "ec")
    }
//  TODO:End

  override def generate(jwt: SignedJWT, audience: List[String]): Future[String] = Future.fromTry {
    for {
      key    <- getPrivateKey(jwt.getHeader.getAlgorithm)
      seed   <- TokenSeed.create(jwt, key, audience)
      token  <- createToken(seed)
      signer <- getSigner(seed.algorithm, key)
      signed <- signToken(token, signer)
      _ = logger.info("Token generated")
    } yield toBase64(signed)
  }

  def getPrivateKey(algorithm: JWSAlgorithm): Try[JWK] = {
    val key = algorithm match {
      case JWSAlgorithm.RS256 | JWSAlgorithm.RS384 | JWSAlgorithm.RS512                       => rsaPrivateKey
      case JWSAlgorithm.PS256 | JWSAlgorithm.PS384 | JWSAlgorithm.PS256                       => rsaPrivateKey
      case JWSAlgorithm.ES256 | JWSAlgorithm.ES384 | JWSAlgorithm.ES512 | JWSAlgorithm.ES256K => ecPrivateKey
      case JWSAlgorithm.EdDSA                                                                 => ecPrivateKey

    }
    key.flatMap(readPrivateKeyFromString)
  }

  private def createToken(seed: TokenSeed): Try[SignedJWT] = Try {
    val issuedAt: Date       = new Date(seed.issuedAt)
    val notBeforeTime: Date  = new Date(seed.nbf)
    val expirationTime: Date = new Date(seed.expireAt)

    val header: JWSHeader = new JWSHeader.Builder(seed.algorithm)
      .customParam("use", "sig")
      .`type`(JOSEObjectType.JWT)
      .keyID(seed.kid)
      .build()

    val payload: JWTClaimsSet = new JWTClaimsSet.Builder()
      .issuer(seed.issuer)
      .audience(seed.audience.asJava)
      .subject(seed.clientId)
      .issueTime(issuedAt)
      .notBeforeTime(notBeforeTime)
      .expirationTime(expirationTime)
      .build()

    new SignedJWT(header, payload)

  }

  def getSigner(algorithm: JWSAlgorithm, key: JWK): Try[JWSSigner] = {
    algorithm match {
      case JWSAlgorithm.RS256 | JWSAlgorithm.RS384 | JWSAlgorithm.RS512                       => rsa(key)
      case JWSAlgorithm.PS256 | JWSAlgorithm.PS384 | JWSAlgorithm.PS256                       => rsa(key)
      case JWSAlgorithm.ES256 | JWSAlgorithm.ES384 | JWSAlgorithm.ES512 | JWSAlgorithm.ES256K => ec(key)
      case JWSAlgorithm.EdDSA                                                                 => octect(key)

    }
  }

  private def readPrivateKeyFromString(keyString: String): Try[JWK] = Try {
    JWK.parse(keyString)
  }

  private def rsa(jwk: JWK): Try[JWSSigner] = Try(new RSASSASigner(jwk.toRSAKey))

  private def ec(jwk: JWK): Try[JWSSigner] = Try(new ECDSASigner(jwk.toECKey))

  private def octect(jwk: JWK): Try[JWSSigner] = Try(new Ed25519Signer(jwk.toOctetKeyPair))

  private def signToken(jwt: SignedJWT, signer: JWSSigner): Try[SignedJWT] = Try {
    val _ = jwt.sign(signer)
    jwt
  }

  @SuppressWarnings(Array("org.wartremover.warts.StringPlusAny"))
  private def toBase64(jwt: SignedJWT): String = {
    s"""${jwt.getHeader.toBase64URL}.${jwt.getPayload.toBase64URL}.${jwt.getSignature}"""
  }

  private def getPrivateKeyFromVault(path: String, key: String): Try[String] = {
    vaultService
      .getSecret(path)
      .get(key)
      .map(decodeBase64)
      .toRight(new RuntimeException("PDND private key not found"))
      .toTry
  }

}
