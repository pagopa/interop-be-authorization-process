package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import com.nimbusds.jose.crypto.{ECDSAVerifier, RSASSAVerifier}
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.{JWSAlgorithm, JWSVerifier}
import com.nimbusds.jwt.SignedJWT
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.Validation
import it.pagopa.pdnd.interop.uservice.authorizationprocess.error.InvalidJWTSign
import it.pagopa.pdnd.interop.uservice.keymanagement.client.model.{Key, OtherPrimeInfo}
import spray.json.{DefaultJsonProtocol, RootJsonFormat, _}

import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

package object impl extends DefaultJsonProtocol with SprayJsonSupport with Validation {

  implicit val otherPrimeInfoFormat: RootJsonFormat[OtherPrimeInfo] = jsonFormat3(OtherPrimeInfo.apply)
  implicit val keyFormat: RootJsonFormat[Key]                       = jsonFormat22(Key.apply)

  implicit def toEntityMarshallerKey: ToEntityMarshaller[Key] = sprayJsonMarshaller[Key]

  def extractJwtInfo(
    clientAssertion: String,
    clientAssertionType: String,
    grantType: String,
    clientId: Option[UUID]
  ): Future[(SignedJWT, String, String)] =
    Future.fromTry {
      for {
        _       <- validateAccessTokenRequest(clientAssertionType, grantType)
        jwt     <- Try(SignedJWT.parse(clientAssertion))
        subject <- Try(jwt.getJWTClaimsSet.getSubject)
        clientId <- Either
          .cond(
            subject == clientId.map(_.toString).getOrElse(subject),
            subject,
            new RuntimeException(s"ClientId ${clientId.toString} not equal to subject $subject")
          )
          .toTry
        kid <- Try(jwt.getHeader.getKeyID)
      } yield (jwt, kid, clientId)
    }

  def getVerifier(algorithm: JWSAlgorithm, key: Key): Future[JWSVerifier] = algorithm match {
    case JWSAlgorithm.RS256 | JWSAlgorithm.RS384 | JWSAlgorithm.RS512 => rsa(key)
    case JWSAlgorithm.ES256                                           => ec(key)
    case _                                                            => Future.failed(new RuntimeException("Invalid key algorithm"))

  }

  def verify(verifier: JWSVerifier, jwt: SignedJWT): Future[SignedJWT] = Future.fromTry {
    {
      Either
        .cond(jwt.verify(verifier), jwt, InvalidJWTSign)
        .toTry
    }
  }

  def rsa(key: Key): Future[RSASSAVerifier] = Future.fromTry {
    Try {
      val jwkTxt: String = key.toJson.compactPrint
      val jwk: JWK       = JWK.parse(jwkTxt)
      val publicKey      = jwk.toRSAKey
      new RSASSAVerifier(publicKey)
    }
  }

  def ec(key: Key): Future[ECDSAVerifier] = Future.fromTry {
    Try {
      val jwkTxt: String = key.toJson.compactPrint
      val jwk: JWK       = JWK.parse(jwkTxt)
      val publicKey      = jwk.toECKey
      new ECDSAVerifier(publicKey)
    }
  }

}
