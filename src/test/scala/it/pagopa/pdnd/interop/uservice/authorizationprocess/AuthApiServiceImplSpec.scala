//package it.pagopa.pdnd.interop.uservice.authorizationprocess
//
//import akka.http.scaladsl.Http
//import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
//import akka.http.scaladsl.marshalling.{Marshal, ToEntityMarshaller}
//import akka.http.scaladsl.model._
//import akka.http.scaladsl.server.directives.SecurityDirectives
//import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshal}
//import com.nimbusds.jose.{JWSAlgorithm, JWSHeader, JWSVerifier}
//import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
//import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.AuthApi
//import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.{
//  AuthApiMarshallerImpl,
//  AuthApiServiceImpl,
//  accessTokenRequestFormat,
//  clientCredentialsResponseFormat,
//  problemFormat
//}
//import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.system.{
//  Authenticator,
//  classicActorSystem,
//  executionContext
//}
//import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.{
//  AccessTokenRequest,
//  ClientCredentialsResponse,
//  Problem
//}
//import it.pagopa.pdnd.interop.uservice.authorizationprocess.server.Controller
//import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl.{JWTGeneratorImpl, JWTValidatorImpl}
//import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{
//  JWTGenerator,
//  JWTValidator,
//  KeyManager,
//  VaultService
//}
//import org.scalamock.scalatest.MockFactory
//import org.scalatest.matchers.must.Matchers
//import org.scalatest.wordspec.AnyWordSpec
//import org.scalatest.{BeforeAndAfterAll, OneInstancePerTest, TestSuite}
//import spray.json.DefaultJsonProtocol
//
//import java.time.Instant
//import java.time.temporal.ChronoUnit
//import scala.util.Try
////import java.time.{Clock, Instant}
//import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl.getVerifier
//
//import java.util.UUID
//import scala.concurrent.duration.{Duration, DurationInt}
//import scala.concurrent.{Await, Future}
//import scala.io.{BufferedSource, Source}
//import scala.jdk.CollectionConverters._
//class AuthApiServiceImplSpec
//    extends AnyWordSpec
//    with TestSuite
//    with Matchers
//    with MockFactory
//    with BeforeAndAfterAll
//    with OneInstancePerTest {
//
//  final lazy val keyPath: String = "src/test/resources/keys"
//
//  import AuthApiServiceImplSpec._
//
//  val vaultService: VaultService = mock[VaultService]
//  val keyManager: KeyManager     = mock[KeyManager]
//
//  var controller: Option[Controller]                 = None
//  var bindServer: Option[Future[Http.ServerBinding]] = None
//
//  val privatePdndRSAKeySource: BufferedSource = Source.fromFile(s"$keyPath/pdnd-test-keypair.rsa.priv.bs64")
//  val privatePdndECKeySource: BufferedSource  = Source.fromFile(s"$keyPath/pdnd-test-private.ec.pem.bs64")
//
//  val privateClientRSAKeySource: BufferedSource = Source.fromFile(s"$keyPath/client-test-keypair.rsa.priv.bs64")
//  val publicClientRsaKeySource: BufferedSource  = Source.fromFile(s"$keyPath/client-test-keypair.rsa.pub.bs64")
//
//  val privateClientECKeySource: BufferedSource = Source.fromFile(s"$keyPath/client-test-private.ec.pem.bs64")
//  val publicClientECKeySource: BufferedSource  = Source.fromFile(s"$keyPath/client-test-public.ec.pem.bs64")
//
//  val privateRSAPdndKey: String = privatePdndRSAKeySource.getLines().mkString
//  val privatePdndECKey: String  = privatePdndECKeySource.getLines().mkString
//
//  val privateClientRSAKey: String = privateClientRSAKeySource.getLines().mkString
//  val privateClientECKey: String  = privateClientECKeySource.getLines().mkString
//
//  val publicClientRsaKey: String = publicClientRsaKeySource.getLines().mkString
//  val publicClientECKey: String  = publicClientECKeySource.getLines().mkString
//
//  override def beforeAll(): Unit = {
//    (vaultService.getSecret _)
//      .expects("pdndInteropRsaPrivateKey")
//      .returning(Map("private" -> privateRSAPdndKey))
//      .once()
//
//    (vaultService.getSecret _)
//      .expects("pdndInteropEcPrivateKey")
//      .returning(Map("private" -> privatePdndECKey))
//      .once()
//
//    (vaultService.getSecret _)
//      .expects(s"secret/data/pdnd-interop-dev/keys/organizations/$clientId/keys/$rsaKid")
//      .returning(Map("public" -> publicClientRsaKey))
//      .repeat(6)
//
//    (vaultService.getSecret _)
//      .expects(s"secret/data/pdnd-interop-dev/keys/organizations/$clientId/keys/$ecKid")
//      .returning(Map("public" -> publicClientECKey))
//      .repeat(4)
//
//    val jwtValidator: JWTValidator = JWTValidatorImpl(keyManager)
//    val jwtGenerator: JWTGenerator = JWTGeneratorImpl(vaultService)
//
//    val authApi: AuthApi = new AuthApi(
//      new AuthApiServiceImpl(jwtValidator, jwtGenerator),
//      new AuthApiMarshallerImpl(),
//      SecurityDirectives.authenticateBasic("SecurityRealm", Authenticator)
//    )
//
//    controller = Some(new Controller(authApi))
//
//    controller foreach { controller =>
//      bindServer = Some(
//        Http()
//          .newServerAt("0.0.0.0", 8088)
//          .bind(controller.routes)
//      )
//
//      Await.result(bindServer.get, 100.seconds)
//    }
//
//  }
//
//  override def afterAll(): Unit = {
//    privatePdndRSAKeySource.close()
//    privatePdndECKeySource.close()
//    privateClientRSAKeySource.close()
//    privateClientRSAKeySource.close()
//    privateClientECKeySource.close()
//    publicClientECKeySource.close()
//    bindServer.foreach(_.foreach(_.unbind()))
//
//  }
//
//  "Working with authorization" must {
//    "return 200 for RS256 token request" in {
//
//      val response: HttpResponse = getServiceResponse("RS256", privateClientRSAKey, rsaKid)
//
//      val body: ClientCredentialsResponse =
//        Await.result(Unmarshal(response.entity).to[ClientCredentialsResponse], Duration.Inf)
//
//      response.status mustBe StatusCodes.OK
//      Try(SignedJWT.parse(body.access_token)).isSuccess mustBe true
//    }
//
//    "return 200 for RS384 token request" in {
//
//      val response: HttpResponse = getServiceResponse("RS384", privateClientRSAKey, rsaKid)
//
//      val body: ClientCredentialsResponse =
//        Await.result(Unmarshal(response.entity).to[ClientCredentialsResponse], Duration.Inf)
//
//      response.status mustBe StatusCodes.OK
//      Try(SignedJWT.parse(body.access_token)).isSuccess mustBe true
//    }
//
//    "return 200 for RS512 token request" in {
//
//      val response: HttpResponse = getServiceResponse("RS512", privateClientRSAKey, rsaKid)
//
//      val body: ClientCredentialsResponse =
//        Await.result(Unmarshal(response.entity).to[ClientCredentialsResponse], Duration.Inf)
//
//      response.status mustBe StatusCodes.OK
//      Try(SignedJWT.parse(body.access_token)).isSuccess mustBe true
//    }
//
//    "return 200 for ES256 token request" in {
//
//      val response: HttpResponse = getServiceResponse("ES256", privateClientECKey, ecKid)
//
//      val body: ClientCredentialsResponse =
//        Await.result(Unmarshal(response.entity).to[ClientCredentialsResponse], Duration.Inf)
//
//      response.status mustBe StatusCodes.OK
//      Try(SignedJWT.parse(body.access_token)).isSuccess mustBe true
//    }
//
//    "return 200 for ES256K token request" in {
//      val response: HttpResponse = getServiceResponse("ES256K", privateClientECKey, ecKid)
//
//      val body: ClientCredentialsResponse =
//        Await.result(Unmarshal(response.entity).to[ClientCredentialsResponse], Duration.Inf)
//
//      response.status mustBe StatusCodes.OK
//
//      Try(SignedJWT.parse(body.access_token)).isSuccess mustBe true
//    }
//
//    "return 200 for ES384 token request" in {
//      val response: HttpResponse = getServiceResponse("ES384", privateClientECKey, ecKid)
//
//      val body: ClientCredentialsResponse =
//        Await.result(Unmarshal(response.entity).to[ClientCredentialsResponse], Duration.Inf)
//
//      response.status mustBe StatusCodes.OK
//      Try(SignedJWT.parse(body.access_token)).isSuccess mustBe true
//    }
//
//    "return 200 for ES512 token request" in {
//      val response: HttpResponse = getServiceResponse("ES512", privateClientECKey, ecKid)
//
//      val body: ClientCredentialsResponse =
//        Await.result(Unmarshal(response.entity).to[ClientCredentialsResponse], Duration.Inf)
//
//      response.status mustBe StatusCodes.OK
//      Try(SignedJWT.parse(body.access_token)).isSuccess mustBe true
//    }
//
//    "fails if the client assertion 'not before' is in future" in {
//
//      val issued: Instant    = Instant.now()
//      val notBefore: Instant = issued.plus(60, ChronoUnit.SECONDS)
//      val expireIn: Instant  = issued.plus(60, ChronoUnit.SECONDS)
//
//      val response = getServiceResponse("RS256", privateClientRSAKey, rsaKid, issued, notBefore, expireIn)
//
//      val body: Problem = Await.result(Unmarshal(response.entity).to[Problem], Duration.Inf)
//
//      response.status mustBe StatusCodes.Unauthorized
//      body.title mustBe "Invalid claim found"
//      body.detail.exists(_.startsWith("The Token can't be used before ")) mustBe true
//    }
//
//    "fails if the client assertion is expired" in {
//
//      val issued: Instant    = Instant.now().minus(60, ChronoUnit.SECONDS)
//      val notBefore: Instant = issued
//      val expireIn: Instant  = issued.plus(10, ChronoUnit.SECONDS)
//
//      val response = getServiceResponse("RS256", privateClientRSAKey, rsaKid, issued, notBefore, expireIn)
//
//      val body: Problem = Await.result(Unmarshal(response.entity).to[Problem], Duration.Inf)
//
//      response.status mustBe StatusCodes.Unauthorized
//      body.title mustBe "Token expired"
//      body.detail.exists(_.startsWith("The Token has expired on ")) mustBe true
//    }
//
//  }
//}
//
//object AuthApiServiceImplSpec extends SprayJsonSupport with DefaultJsonProtocol {
//  implicit def toEntityMarshallerAccessTokenRequest: ToEntityMarshaller[AccessTokenRequest] =
//    sprayJsonMarshaller[AccessTokenRequest]
//
//  implicit def fromEntityUnmarshallerClientCredentialsResponse: FromEntityUnmarshaller[ClientCredentialsResponse] =
//    sprayJsonUnmarshaller[ClientCredentialsResponse]
//
//  implicit def fromEntityUnmarshallerProblem: FromEntityUnmarshaller[Problem] = sprayJsonUnmarshaller[Problem]
//
//  final lazy val url: String    = "http://localhost:8088/pdnd-interop-uservice-authorization-process/0.0.1"
//  final val rsaKid: String      = "1234567890"
//  final val ecKid: String       = "1234567891"
//  final val clientId: String    = "f9f4e043-a9ed-4574-8310-d1fbe1c1b226"
//  final val agreementId: String = "f9f4e043-a9ed-4574-8310-d1fbe1c1b100"
//  final val jti: String         = "b39216dc-56d1-4d1d-86c2-d8fa86e81732"
//  final val issued: Instant     = Instant.now()
//  final val expireIn: Instant   = issued.plus(60, ChronoUnit.SECONDS)
//
//  def createPayload(issued: Instant, notBefore: Instant, expireIn: Instant): Map[String, AnyRef] = Map(
//    "iss" -> clientId,
//    "sub" -> clientId,
//    "jti" -> jti,
//    "aud" -> "https://gateway.interop.pdnd.dev/auth",
//    "iat" -> issued.getEpochSecond,
//    "nbf" -> notBefore.getEpochSecond,
//    "exp" -> expireIn.getEpochSecond
//  )
//
//  def getServiceResponse(
//    alg: String,
//    privateKey: String,
//    kid: String,
//    issued: Instant = issued,
//    notBefore: Instant = issued,
//    expireIn: Instant = expireIn
//  ): HttpResponse = {
//    val algorithm: JWSAlgorithm = getVerifier(JWSAlgorithm.parse(alg), _)
//    val signer                  = JWSVerifier
//    val payload                 = createPayload(issued, notBefore, expireIn)
//
//    val header: JWSHeader       = new JWSHeader.Builder(algorithm).keyID(kid).build()
//    val claimsSet: JWTClaimsSet = JWTClaimsSet.parse(payload.asJava)
//    val assertion: SignedJWT    = new SignedJWT(header, claimsSet).sign()
//
//    val request = AccessTokenRequest(
//      client_id = Some(UUID.fromString(clientId)),
//      client_assertion = assertion.toString,
//      client_assertion_type = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
//      grant_type = "client_credentials",
//      audience = UUID.fromString(agreementId)
//    )
//
//    val data = Await.result(Marshal(request).to[MessageEntity].map(_.dataBytes), Duration.Inf)
//
//    val response = Await.result(
//      Http().singleRequest(
//        HttpRequest(
//          uri = s"$url/as/token.oauth2",
//          method = HttpMethods.POST,
//          entity = HttpEntity(ContentTypes.`application/json`, data)
//        )
//      ),
//      Duration.Inf
//    )
//
//    response
//
//  }
//
//}
