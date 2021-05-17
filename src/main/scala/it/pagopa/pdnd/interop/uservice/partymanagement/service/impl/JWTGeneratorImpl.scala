package it.pagopa.pdnd.interop.uservice.partymanagement.service.impl

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.bettercloud.vault.Vault
import com.bettercloud.vault.response.LogicalResponse
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.decodeBase64
import it.pagopa.pdnd.interop.uservice.partymanagement.model.token.TokenSeed
import it.pagopa.pdnd.interop.uservice.partymanagement.service.JWTGenerator
import org.slf4j.{Logger, LoggerFactory}

import java.util.Date
import scala.jdk.CollectionConverters.MapHasAsScala
import scala.util.{Failure, Try}

class JWTGeneratorImpl(vault: Vault) extends JWTGenerator {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

//   TODO: Start
//   TODO: this part is static and initialized at the start up
//   TODO - use a def instead a val, but this approach generate to many calls to the vault
//   TODO - use a refreshing cache, more complex
  private val rsaPrivateKey: Try[String] =
    Try(System.getenv("PDND_INTEROP_RSA_PRIVATE_KEY")).flatMap(keyPath => getPrivateKeyFromVault(keyPath))

  private val ecPrivateKey: Try[String] =
    Try(System.getenv("PDND_INTEROP_EC_PRIVATE_KEY")).flatMap(keyPath => getPrivateKeyFromVault(keyPath))
//  TODO:End

  override def generate(seed: TokenSeed): Try[String] = for {
    pk        <- getPrivateKey(seed.algorithm)
    algorithm <- generateAlgorithm(seed.algorithm, pk)
    _ = logger.info("Generating token")
    token <- createToken(algorithm, seed)
    _ = logger.info("Token generated")
  } yield token

  private def createToken(algorithm: Algorithm, seed: TokenSeed): Try[String] = Try {
    val issuedAt: Date = new Date(seed.issuedAt)
    JWT.create
      .withJWTId(seed.id.toString)
      .withKeyId(seed.kid)
      .withSubject(seed.clientId)
      .withIssuer(seed.issuer)
      .withIssuedAt(issuedAt)
      .withNotBefore(issuedAt)
      .withExpiresAt(new Date(seed.expireAt))
      .sign(algorithm)
  }

  private def getPrivateKey(algorithm: String): Try[String] = {
    logger.info(algorithm)
    algorithm match {
      case alg if alg.startsWith("RS") => {
        rsaPrivateKey.foreach(logger.info)
        rsaPrivateKey
      }
      case alg if alg.startsWith("ES") => {
        ecPrivateKey.foreach(logger.info)
        ecPrivateKey
      }
      case _ => Failure(new RuntimeException("PDND private key not found"))
    }
  }

  private def getPrivateKeyFromVault(path: String): Try[String] = {
    val data: LogicalResponse = vault.logical().read(path)
    data.getData.asScala
      .get("private")
      .map(decodeBase64)
      .toRight(new RuntimeException("PDND private key not found"))
      .toTry
  }

}
