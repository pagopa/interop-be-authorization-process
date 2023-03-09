package it.pagopa.interop.authorizationprocess.util

import com.nimbusds.jose.crypto.{ECDSASigner, RSASSASigner}
import com.nimbusds.jose.jwk.gen.{ECKeyGenerator, RSAKeyGenerator}
import com.nimbusds.jose.jwk.{Curve, ECKey, JWK, RSAKey}
import com.nimbusds.jose.{JWSAlgorithm, JWSHeader}
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import it.pagopa.interop.authorizationprocess.common.system.ApplicationConfiguration
import org.bouncycastle.util.io.pem.{PemObject, PemWriter}

import java.io.StringWriter
import java.time.{LocalDate, ZoneId}
import java.util.{Date, UUID}

trait JWTMaker {

  val expirationTime: Date = Date.from(LocalDate.of(2098, 12, 12).atStartOfDay(ZoneId.systemDefault()).toInstant())

  def generateRSAKeys(): (String, String, String) = {
    val rsaJWK: RSAKey = new RSAKeyGenerator(2048).generate
    val kid            = rsaJWK.computeThumbprint().toJSONString
    val public: String = writeKey(rsaJWK.toPublicKey.getEncoded, "PUBLIC")

    (kid, public, rsaJWK.toJSONString)
  }

  def generateECKeys(): (String, String, String) = {
    val ecKey: ECKey   = new ECKeyGenerator(Curve.P_256).generate
    val kid            = ecKey.computeThumbprint().toJSONString
    val public: String = writeKey(ecKey.toPublicKey.getEncoded, "PUBLIC")

    (kid, public, ecKey.toJSONString)
  }

  private def writeKey(encodedKey: Array[Byte], header: String): String = {
    val output    = new StringWriter
    val pemWriter = new PemWriter(output)
    val pem       = new PemObject(s"${header} KEY", encodedKey)
    pemWriter.writeObject(pem)
    pemWriter.close()

    output.toString
  }

  def makeJWT(clientId: String, audience: String, algo: String, kid: String, privateKeyPEM: String): String = {
    // RSA signatures require a public and private RSA key pair,
    // EC signatures require a public and private EC key pair,

    val now = new Date();
    val jwk = JWK.parse(privateKeyPEM)

    // Create signer with the private key
    val signer = if (algo == "RSA") new RSASSASigner(jwk.toRSAKey.toPrivateKey) else new ECDSASigner(jwk.toECKey)

    val claimsSet = new JWTClaimsSet.Builder()
      .issuer(ApplicationConfiguration.getInteropIdIssuer) // TODO Interop issuer uuid
      .subject(clientId)
      .jwtID(UUID.randomUUID.toString)
      .audience(audience)
      .expirationTime(expirationTime)
      .issueTime(now)
      .notBeforeTime(now)
      .build()

    // Prepare JWS object with simple string as payload
    val algorithm = if (algo == "RSA") JWSAlgorithm.RS256 else JWSAlgorithm.ES256

    val jwsObject = new SignedJWT(
      new JWSHeader.Builder(algorithm)
        .keyID(kid)
        .build,
      claimsSet
    )

    // Compute the signature
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

  val (rsaKid, publicRsaKey, privateRsaKey) = generateRSAKeys()
  val (ecKid, publicEcKey, privateEcKey)    = generateECKeys()

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

  val audience = "1a55bd02-a25d-43fe-9a34-fea7b0c871c1"

//  val kidRsa = "mcUXwiCT3D-z8safjBoKE9nqs_7usagK-Ptmvo1nVIg"
//  val privateRSAKeyJSON =
//    """{"p":"wEKHw1qnE1roCwjy6qd-0exuErxSBp9bWREt8xFbR4auVdn1VoiuGQKkzDRHxRE2b5A-2N1u8tEaGrZvgRXDTfuysVHU_2c5dxXIRbyCpqf9fWo83OT67X5_sfLilHK-5Vl1RLZJEB-s0hGlQ2yc5Z7oi9iyCsZ33Iae5asXb78","kty":"RSA","q":"qv921TQkjHHOp9_xut_XQ8XsJP4TVtIbnZvjqj2ipw7jE1TpUoAzt7LUagndRREEE2Mnp4mYiMxMKZqR8WA2vA7ZCjeTdiGHqt7oDjHAmzOxh0VLjAusM92kN4z6lUXByUNx0Qi4CTMSyT9TbhgH5G6mI6VrEU3mTl8Pxs9C-Js","d":"QJeWiaa52xdnHLRor4UvwZ5TfwkaMhHlXCgLrYJPD3llpQP-W0suIwWo8Pui5z_97zrUqPBdUoesL2bA_XQrfNPvYG1nhc4gl-A7FCwg813KyGwI8APTLDDt1FJ53C0I8d9_xj-V1Bj-5Bt05UK5Nn3JG4zJ6h5blK9vnFdYB3e6wOF2gp6eTt1-jxzVV70bW4NjRuD_03wyCiaygspkj1rfNV6l09RfH65k0c9GydeBcc0Z2wyZWNEAUy1UnZxBUb6srF_y1M-BGiO-ftQvPGQSZko16q4kIU3kRTBI2M_dGTDchNkQcs1w20x6V5K71YMPFxN97_I4FooDxjcR5Q","e":"AQAB","qi":"CKxSpZt0k9BgtbfKetePnb2KulxI0yB_ky2AStcu34NviTWAZbE6NI5fNvkR8zigvUM6foAdNPwvDMHZS6EphKFkxfMAsGtYeV8_jnPC9h7Ekcvey7xvcjm8ISJCOHJP37XOWXIZw2dFu_o8R_fFmEtq0HoyaZR8daZGtVtiIvM","dp":"mU90O096k3CWQNZt_rh55KQIUmBheG5yxV9xqLZad3rqYgNgJBTx33fAOiYmZPsI0YXQ19Ybtv0PN-XqnKDiELl5EPUUSGXj6RYxkYp1FLg4511kEzF09xU8doYcMAwgNXtUi-pf8L-RbCIuCsn9gw1omru9neINioi_BJ2eHrk","dq":"ogcM_7q3wwh3q-RsNgmx_PsG5oqFoqfWGQLEt-RNQgS-L-wuZckquC7QTWXpb29PMFutEHg1u7HxnR5kmZX0Zz-ecqr0pGPjHIq40fJcsfNKjYWgryPEWST0XNrN-jGuDNpGd67OS5FEhMLneBN3LwGVlYNBr5Tj3HEBDv4HVG8","n":"gGwJq77iNwK2RoeYNfmEbdzL3CfN0skQjOQsD_ufzhzqMA4RUDLlUwsVCzoNVXWZNweyDkhKZHTzQ4z4BpBSBQspZ6WphijqdtCkDt_BXmeqXuEXzdXycLCHHKrv88BzDoi--JAMtANoyJMh29pOXwk3KKPhRKH8nzt0BM6WBRUzkBAYrcavLj-3BddUuZRw5KkRL6VLddRbcj6z8Xww7_qKJyAroOKlWaIo3O9TKTZ1sjTpQ5Rlu5EBfSt6BFjjZ2hAbQ7kZWGxLckJjrYu57VQrH5C0IQ0MFisRU18rFKojEBfJJt6uhfyDk7rcIGecnq5G7wl0mb5jGDti6-wpQ"}""".stripMargin
//
//  val kidEc = "mcUXwiCT3D-z8safjBoKE9nqs_7usagK-Ptmvo1nVIg"
//  val privateEcKeyJSON =
//    """{"kty":"EC","d":"4w_IvomaTMXlrvzpwjF5ZnWxc80hfeZMtPwQfwkgkF4","crv":"P-256","x":"_Jg0lLnqrYwcGcj9qB07z-mr85dzjf0KAfIXe9mqXmY","y":"Qf0_Edm3bxP-R5TrZ8ZEwl92YhEj4Fmv0ZlGjiXoLGs"}""".stripMargin

  def printOut(
    kind: String,
    clientId: String,
    audience: String,
    publicKey: String,
    privateKey: String,
    kid: String
  ): Unit = {
    val delimiter = "=".repeat(100)
    println(delimiter)
    println(s"Kid $kind:")
    println(ecKid)
    println()
    println(s"Public $kind:")
    println(publicKey)
    println(s"Private $kind: ")
    println(privateKey)
    println()
    println(s"JWT $kind:")
    println(makeJWT(clientId, audience, kind, kid, privateKey))
    println(delimiter)
  }

  printOut("RSA", clientId, audience, publicRsaKey, privateRsaKey, rsaKid)
  printOut("EC", clientId, audience, publicEcKey, privateEcKey, ecKid)

}
