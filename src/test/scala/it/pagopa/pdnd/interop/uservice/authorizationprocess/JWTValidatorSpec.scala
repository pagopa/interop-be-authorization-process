package it.pagopa.pdnd.interop.uservice.authorizationprocess

import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl.JWTValidatorImpl
import it.pagopa.pdnd.interop.uservice.authorizationprocess.util.{JWTMaker, SpecUtils}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.ListHasAsScala

class JWTValidatorSpec
    extends AnyWordSpecLike
    with MockFactory
    with SpecUtils
    with JWTMaker
    with ScalaFutures
    with Matchers {

  val jwtValidator = JWTValidatorImpl(mockAuthorizationManagementService, mockVaultService)(ExecutionContext.global)

  "JWTValidator" should {
    "produce a valid jwt claim set" in {

      val rsaJWK: RSAKey       = new RSAKeyGenerator(2048).generate
      val rsaKid               = rsaJWK.computeThumbprint().toJSONString
      val publicRsaKey: String = rsaJWK.toPublicJWK.toJSONString
      val privateRsaKey        = rsaJWK.toJSONString

      val clientId = "e58035ce-c753-4f72-b613-46f8a17b71cc"
      val audience = "1a55bd02-a25d-43fe-9a34-fea7b0c871c1"
      val bearer   = makeJWT(clientId, audience, "RSA", rsaKid, privateRsaKey)

      (mockVaultService.readBase64EncodedData _)
        .expects(*)
        .returns(Map(rsaKid -> publicRsaKey))
        .once()

      val claims = jwtValidator.validateBearer(bearer).futureValue

      claims.getSubject shouldBe clientId
      claims.getAudience.asScala shouldBe List(audience)
    }
  }

}
