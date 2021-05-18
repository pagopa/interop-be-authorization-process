package it.pagopa.pdnd.interop.uservice.partymanagement

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.{Marshal, ToEntityMarshaller}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.directives.SecurityDirectives
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.AuthApi
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.AccessTokenRequest
import it.pagopa.pdnd.interop.uservice.authorizationprocess.server.Controller
import it.pagopa.pdnd.interop.uservice.partymanagement.api.impl.{
  AuthApiMarshallerImpl,
  AuthApiServiceImpl,
  accessTokenRequestFormat
}
import it.pagopa.pdnd.interop.uservice.partymanagement.common.system.{
  Authenticator,
  classicActorSystem,
  executionContext
}
import it.pagopa.pdnd.interop.uservice.partymanagement.common.utils.decodeBase64
import it.pagopa.pdnd.interop.uservice.partymanagement.service.impl.{
  JWTGeneratorImpl,
  JWTValidatorImpl,
  generateAlgorithm
}
import it.pagopa.pdnd.interop.uservice.partymanagement.service.{JWTGenerator, JWTValidator, VaultService}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, OneInstancePerTest, TestSuite}
import spray.json.DefaultJsonProtocol

import java.time.Instant
import java.time.temporal.ChronoUnit
//import java.time.{Clock, Instant}
import java.util.UUID
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, Future}
import scala.io.{BufferedSource, Source}
import scala.jdk.CollectionConverters._

@SuppressWarnings(
  Array(
    "org.wartremover.warts.OptionPartial",
    "org.wartremover.warts.Var",
    "org.wartremover.warts.Any",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.Null",
    "org.wartremover.warts.TryPartial"
  )
)
class AuthApiServiceImplSpec
    extends AnyWordSpec
    with TestSuite
    with Matchers
    with MockFactory
    with BeforeAndAfterAll
    with OneInstancePerTest {
  final lazy val url: String     = "http://localhost:8088/pdnd-interop-uservice-authorization-process/0.0.1"
  final lazy val keyPath: String = "src/test/resources/keys"
  import AuthApiServiceImplSpec._

  val vaultService: VaultService = mock[VaultService]

  var controller: Option[Controller]                 = None
  var bindServer: Option[Future[Http.ServerBinding]] = None

  val privatePdndRSAKeySource: BufferedSource   = Source.fromFile(s"$keyPath/pdnd-test-keypair.rsa.priv.bs64")
  val privatePdndECKeySource: BufferedSource    = Source.fromFile(s"$keyPath/pdnd-test-private.ec.key.bs64")
  val privateClientRSAKeySource: BufferedSource = Source.fromFile(s"$keyPath/client-test-keypair.rsa.priv.bs64")
  val privateClientECKeySource: BufferedSource  = Source.fromFile(s"$keyPath/client-test-private.ec.pem.bs64")
  val publicRsaKeySource: BufferedSource        = Source.fromFile(s"$keyPath/client-test-keypair.rsa.pub.bs64")
  val publicECKeySource: BufferedSource         = Source.fromFile(s"$keyPath/client-test-public.ec.pem.bs64")

  val privateRSAPdndKey: String   = privatePdndRSAKeySource.getLines().mkString
  val privatePdndECKey: String    = privatePdndECKeySource.getLines().mkString
  val privateClientRSAKey: String = privateClientRSAKeySource.getLines().mkString
  val privateClientECKey: String  = privateClientECKeySource.getLines().mkString

  val publicClientRsaKey: String = publicRsaKeySource.getLines().mkString
  val publicClientECKey: String  = publicECKeySource.getLines().mkString

  override def beforeAll(): Unit = {
    (vaultService.getSecret _)
      .expects("pdndInteropRsaPrivateKey")
      .returning(Map("private" -> privateRSAPdndKey))
      .once()

    (vaultService.getSecret _)
      .expects("pdndInteropEcPrivateKey")
      .returning(Map("private" -> privatePdndECKey))
      .once()

    (vaultService.getSecret _)
      .expects(s"secret/data/pdnd-interop-dev/keys/organizations/$clientId/keys/$rsaKid")
      .returning(Map("public" -> publicClientRsaKey))
      .repeat(2)

    val jwtValidator: JWTValidator = new JWTValidatorImpl(vaultService)
    val jwtGenerator: JWTGenerator = new JWTGeneratorImpl(vaultService)

    val authApi: AuthApi = new AuthApi(
      new AuthApiServiceImpl(jwtValidator, jwtGenerator),
      new AuthApiMarshallerImpl(),
      SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
    )

    controller = Some(new Controller(authApi))

    controller foreach { controller =>
      bindServer = Some(
        Http()
          .newServerAt("0.0.0.0", 8088)
          .bind(controller.routes)
      )

      Await.result(bindServer.get, 100.seconds)
    }

  }

  override def afterAll(): Unit = {
    privatePdndRSAKeySource.close()
    privatePdndECKeySource.close()
    privateClientRSAKeySource.close()
    privateClientRSAKeySource.close()
    privateClientECKeySource.close()
    publicECKeySource.close()
    bindServer.foreach(_.foreach(_.unbind()))

  }

  val issued: Instant   = Instant.now()
  val expireIn: Instant = issued.plus(60, ChronoUnit.SECONDS)

  val payload: Map[String, Any] = Map(
    "iss" -> "f9f4e043-a9ed-4574-8310-d1fbe1c1b226",
    "sub" -> "f9f4e043-a9ed-4574-8310-d1fbe1c1b226",
    "jti" -> "12342",
    "aud" -> "https://gateway.interop.pdnd.dev/auth",
    "iat" -> issued.getEpochSecond,
    "nbf" -> issued.getEpochSecond,
    "exp" -> expireIn.getEpochSecond
  )

  "Working with authorization" must {
    "return 200" in {

      val algorithm: Algorithm = generateAlgorithm("RS256", decodeBase64(privateClientRSAKey)).get

      val assertion: String = JWT.create().withKeyId(rsaKid).withPayload(payload.asJava).sign(algorithm)

      val request = AccessTokenRequest(
        client_id = UUID.fromString(clientId),
        client_assertion = assertion,
        client_assertion_type = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
        grant_type = "client_credentials",
        agreement_id = UUID.fromString("f9f4e043-a9ed-4574-8310-d1fbe1c1b100")
      )

      val data = Await.result(Marshal(request).to[MessageEntity].map(_.dataBytes), Duration.Inf)

      val response = Await.result(
        Http().singleRequest(
          HttpRequest(
            uri = s"$url/as/token.oauth2",
            method = HttpMethods.POST,
            entity = HttpEntity(ContentTypes.`application/json`, data)
          )
        ),
        Duration.Inf
      )

      response.status mustBe StatusCodes.OK
    }
  }
}

object AuthApiServiceImplSpec extends SprayJsonSupport with DefaultJsonProtocol {
  implicit def toEntityMarshallerAccessTokenRequest: ToEntityMarshaller[AccessTokenRequest] =
    sprayJsonMarshaller[AccessTokenRequest]

  val rsaKid   = "1234567890"
  val clientId = "f9f4e043-a9ed-4574-8310-d1fbe1c1b226"

}
