package it.pagopa.pdnd.interop.uservice.authorizationprocess.util
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.{JWK, RSAKey}
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jose.{JWSAlgorithm, JWSHeader}
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import org.bouncycastle.util.io.pem.{PemObject, PemWriter}

import java.io.StringWriter
import java.time.{LocalDate, ZoneId}
import java.util.{Date, UUID}

trait JWTMaker {

  val expirationTime: Date = Date.from(LocalDate.of(2098, 12, 12).atStartOfDay(ZoneId.systemDefault()).toInstant())

  def generateKeys(): (String, String) = {
    val rsaJWK: RSAKey = new RSAKeyGenerator(2048).generate
    val public: String = writeKey(rsaJWK.toPublicKey.getEncoded, "PUBLIC")

    (public, rsaJWK.toJSONString())
  }

  private def writeKey(encodedKey: Array[Byte], header: String): String = {
    val output    = new StringWriter
    val pemWriter = new PemWriter(output)
    val pem       = new PemObject(s"${header} KEY", encodedKey)
    pemWriter.writeObject(pem)
    pemWriter.close()

    output.toString
  }

  def makeJWT(clientId: String, audience: String, kid: String, privateKeyPEM: String): String = {
    // RSA signatures require a public and private RSA key pair,// RSA signatures require a public and private RSA key pair,

    val now = new Date();
    val jwk = JWK.parse(privateKeyPEM)

    // Create RSA-signer with the private key
    val signer = new RSASSASigner(jwk.toRSAKey.toPrivateKey)

    val claimsSet = new JWTClaimsSet.Builder()
      .issuer("Scala-Test")
      .subject(clientId)
      .jwtID(UUID.randomUUID.toString)
      .audience(audience)
      .expirationTime(expirationTime)
      .issueTime(now)
      .notBeforeTime(now)
      .build()

    // Prepare JWS object with simple string as payload
    val jwsObject = new SignedJWT(
      new JWSHeader.Builder(JWSAlgorithm.RS256)
        .keyID(kid)
        .build,
      claimsSet
    )

    // Compute the RSA signature
    jwsObject.sign(signer)

    // To serialize to compact form, produces something like
    // eyJhbGciOiJSUzI1NiJ9.SW4gUlNBIHdlIHRydXN0IQ.IRMQENi4nJyp4er2L
    // mZq3ivwoAjqa1uUkSBKFIX7ATndFF5ivnt-m8uApHO4kfIFOrW7w2Ezmlg3Qd
    // maXlS9DhN0nUk_hGI3amEjkKd0BWYCB8vfUbUv0XGjQip78AI4z1PrFRNidm7
    // -jPDm5Iq0SZnjKjCNS5Q15fokXZc8u0A
    jwsObject.serialize
  }

}

object JWTSampleTokenGenerator extends App with JWTMaker {

  val (publicKey, privateKey) = generateKeys()

  //println("Public:")
  //println(publicKey)
  //println(" ---- ")
  //println("Private: ")
  //println(privateKey)

  val clientId = "e58035ce-c753-4f72-b613-46f8a17b71cc"

  val publicKeyPEM = """-----BEGIN PUBLIC KEY-----
                       |MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhLqHzj2PX1WC1YZTy9MS
                       |wyRwlObval2H4o9onTcq/tH6ky75Y18HEarlMRGsSLBbZoaMsA52N5wmDBQR7HN5
                       |OFaWeDbY8kbWhTyAXCcfX2hMjow2Wb0d9GfY73dY0LIvH3bXE+mQTw+tWArUmGKt
                       |5faihtO1lmRx3J/PBbaM71RFobNv94kf4QgwITU6P3NRIxnCIZi7+uTItcIJF3YW
                       |uL3uPXNcX5Li0DI1JQUjhd0bHjkRO3/uFuFYe9kFdgWtSFoFT7bo3PqucaxChjbX
                       |dJ9bIxQg00KZWUBHXlwlC7RpMxSJW/c3RjIfsYx2E6uhROFcga59goUgArW6OWRM
                       |GQIDAQAB
                       |-----END PUBLIC KEY-----""".stripMargin

  val kid      = "OOPgXEwMztaf_VjKwjekSkL3zGUWIJGJtcXHSRFgSpE"
  val audience = "1a55bd02-a25d-43fe-9a34-fea7b0c871c1"

  val privateKeyJSON =
    """{
      |    "p": "xrZZRpoFjrpuxtn--vwtOZigbWPmvjhNd0txev2LlYtgkZS2rZpYP8fwRF74ommOZci4FxYzewD6Is2IGrF0lOddXnfiOV74oXU04Sw64Pe8l-74dYyYVdBHRv5-jf1SKiGKKtRgBFfX2Q81LCny7bYt8IvE1_yYVLwfCfJ2Fls",
      |    "kty": "RSA",
      |    "q": "qv5cPN4c55sy0hVtyc5z6a_Tzui1CpM6jbSQDqfKllq_RDch7vXB-u1C7OsBb7JOELB75rOzNQ7m9yuj2EdSWbZAQ5SKZjpHM3OVO4ZE1owTzl9rnIHaMOl2RLIcD2IcH1OxLZow5MH1xaVPwfo6Tv9AD72WqETEriABdS_HuZs",
      |    "d": "Pfj6QAnuT40Vsa0uoxTCxerVxjCtyPQy5k4fgoinwqM4ZFCikQtluZIZwXeHAcmWY6CfP_UAraZy_WDBna2tA-kqCKdTVIxOzZcaN-zscRe55zUAi5YJznxOhkErbwZDimVqtxlvD_s57MnhyeUJXT5zVJC1UKtFnsXfW8OIupXp7dYB_tpof3Y_UjdO4GTilByFCJWy1oJxvwNKfnb4pa67qw-eT7j-hNxdaIEaeGwkdAiSs4_hrlMm3M0CvQf27rFXh6u_2af8ujUdhGUUvNNk7Ynm225rw56_EroyJvX3DCXE-pOyHHgw4fQKPIC_Y1txy7q0FSAtvD4hgQWvIQ",
      |    "e": "AQAB",
      |    "qi": "Otq53saBA3piQMVhgzmyZlYdhZJgsur92aQURZ_lMaNqEPEf540tpwfEYO75AEoFt6YC2q95Q-2dIppQuc6NP_F17UHBaRYYjT-07L9tECZNNYW3G92lW9D5oE_qd6_K-VtC6yRPHqqx2QkKsdUI2aWbuQnPHHgFP-I6_LePGLQ",
      |    "dp": "ioEvWPaiSQnJjPEFuQtsumiX6adofc3gsPX08zUmxeWQOejeK8MZH9vMrNtFkm7gwjFVn0HqQCI-N2PrKi_mgqOBgQcut65qvp9jbE_X-lazLXNz2vtUcvvpsqJQs8eOLa-TDqdZBa301Wa0OURD_0ysWK4TVjjKNMWrHNPTW2E",
      |    "dq": "bqZkt7qfh7xleY8GWYXwejMeZBEwPiShylsisWkg7oTQqmrm2YRMv3zTRw6YAlimraQWuWZlvBrlmOKzhtw4TPdjxJeVq6tgscnEsx0i5JcGphAXSdK5h9c7gh6ji8zYF-mHiNPzecSNrxVXdFXhb4c7RDRSDpdZkrgBWXzOyKs",
      |    "n": "hLqHzj2PX1WC1YZTy9MSwyRwlObval2H4o9onTcq_tH6ky75Y18HEarlMRGsSLBbZoaMsA52N5wmDBQR7HN5OFaWeDbY8kbWhTyAXCcfX2hMjow2Wb0d9GfY73dY0LIvH3bXE-mQTw-tWArUmGKt5faihtO1lmRx3J_PBbaM71RFobNv94kf4QgwITU6P3NRIxnCIZi7-uTItcIJF3YWuL3uPXNcX5Li0DI1JQUjhd0bHjkRO3_uFuFYe9kFdgWtSFoFT7bo3PqucaxChjbXdJ9bIxQg00KZWUBHXlwlC7RpMxSJW_c3RjIfsYx2E6uhROFcga59goUgArW6OWRMGQ"
      |}""".stripMargin
  println(makeJWT(clientId, audience, kid, privateKeyJSON))

}
