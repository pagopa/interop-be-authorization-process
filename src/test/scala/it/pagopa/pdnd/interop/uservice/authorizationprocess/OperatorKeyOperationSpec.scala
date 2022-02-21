package it.pagopa.pdnd.interop.uservice.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.OperatorApiServiceImpl
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{ManagementClient, PartyManagementService}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.util.SpecUtils
import it.pagopa.interop.authorizationmanagement
import it.pagopa.interop.authorizationmanagement.client.{model => AuthorizationManagementDependency}
import it.pagopa.pdnd.interop.uservice.partymanagement.client.{model => PartyManagementDependency}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class OperatorKeyOperationSpec extends AnyWordSpecLike with MockFactory with SpecUtils with ScalatestRouteTest {
  import clientApiMarshaller._

  val service: OperatorApiServiceImpl =
    OperatorApiServiceImpl(mockAuthorizationManagementService, mockPartyManagementService, mockJwtReader)(
      ExecutionContext.global
    )

  val kid: String = "some-kid"

  val apiClientKey: ClientKey = ClientKey(key =
    Key(
      kty = createdKey.key.kty,
      key_ops = createdKey.key.keyOps,
      use = createdKey.key.use,
      alg = createdKey.key.alg,
      kid = createdKey.key.kid,
      x5u = createdKey.key.x5u,
      x5t = createdKey.key.x5t,
      x5tS256 = createdKey.key.x5tS256,
      x5c = createdKey.key.x5c,
      crv = createdKey.key.crv,
      x = createdKey.key.x,
      y = createdKey.key.y,
      d = createdKey.key.d,
      k = createdKey.key.k,
      n = createdKey.key.n,
      e = createdKey.key.e,
      p = createdKey.key.p,
      q = createdKey.key.q,
      dp = createdKey.key.dp,
      dq = createdKey.key.dq,
      qi = createdKey.key.qi,
      oth = createdKey.key.oth.map(_.map(info => OtherPrimeInfo(r = info.r, d = info.d, t = info.t)))
    )
  )

  val relationship1: PartyManagementDependency.Relationship = PartyManagementDependency.Relationship(
    id = UUID.randomUUID(),
    from = user.id,
    to = UUID.randomUUID(),
    role = PartyManagementDependency.PartyRole.OPERATOR,
    product = PartyManagementDependency
      .RelationshipProduct("PDND", PartyManagementService.PRODUCT_ROLE_SECURITY_OPERATOR, timestamp),
    state = PartyManagementDependency.RelationshipState.ACTIVE,
    createdAt = timestamp
  )

  val relationship2: PartyManagementDependency.Relationship = PartyManagementDependency.Relationship(
    id = UUID.randomUUID(),
    from = user.id,
    to = UUID.randomUUID(),
    role = PartyManagementDependency.PartyRole.OPERATOR,
    product = PartyManagementDependency
      .RelationshipProduct("PDND", PartyManagementService.PRODUCT_ROLE_SECURITY_OPERATOR, timestamp),
    state = PartyManagementDependency.RelationshipState.ACTIVE,
    createdAt = timestamp
  )

  val relationship3: PartyManagementDependency.Relationship = PartyManagementDependency.Relationship(
    id = UUID.randomUUID(),
    from = user.id,
    to = UUID.randomUUID(),
    role = PartyManagementDependency.PartyRole.MANAGER,
    product = PartyManagementDependency.RelationshipProduct("PDND", "admin", timestamp),
    state = PartyManagementDependency.RelationshipState.ACTIVE,
    createdAt = timestamp
  )

  override val relationships: PartyManagementDependency.Relationships =
    PartyManagementDependency.Relationships(Seq(relationship1, relationship2, relationship3))

  val client1: ManagementClient = authorizationmanagement.client.model.Client(
    id = UUID.randomUUID(),
    consumerId = UUID.randomUUID(),
    name = "client1",
    description = None,
    relationships = Set(relationship1.id),
    purposes = Seq(clientPurpose)
  )
  val client2: ManagementClient = authorizationmanagement.client.model.Client(
    id = UUID.randomUUID(),
    consumerId = UUID.randomUUID(),
    name = "client2",
    description = None,
    relationships = Set(relationship2.id),
    purposes = Seq(clientPurpose)
  )

  "Retrieve key" should {
    "succeed" in {

      execForEachOperatorClientExpectations()

      (mockAuthorizationManagementService
        .getKey(_: UUID, _: String)(_: String))
        .expects(client1.id, kid, bearerToken)
        .once()
        .returns(Future.successful(createdKey))

      (mockAuthorizationManagementService
        .getKey(_: UUID, _: String)(_: String))
        .expects(client2.id, kid, bearerToken)
        .once()
        .returns(Future.failed(authorizationmanagement.client.invoker.ApiError(404, "message", None)))

      val expected = apiClientKey

      Get() ~> service.getOperatorKeyById(personId.toString, kid) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[ClientKey] shouldEqual expected
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      val kid                                      = "some-kid"
      Get() ~> service.getOperatorKeyById(personId.toString, kid) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if client or key do not exist" in {
      val kid = "some-kid"

      execForEachOperatorClientExpectations()

      (mockAuthorizationManagementService
        .getKey(_: UUID, _: String)(_: String))
        .expects(client1.id, kid, bearerToken)
        .once()
        .returns(Future.failed(authorizationmanagement.client.invoker.ApiError(404, "message", None)))

      (mockAuthorizationManagementService
        .getKey(_: UUID, _: String)(_: String))
        .expects(client2.id, kid, bearerToken)
        .once()
        .returns(Future.failed(authorizationmanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.getOperatorKeyById(personId.toString, kid) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Retrieve all client keys" should {
    "succeed" in {

      execForEachOperatorClientExpectations()

      (mockAuthorizationManagementService
        .getClientKeys(_: UUID)(_: String))
        .expects(client1.id, bearerToken)
        .once()
        .returns(
          Future.successful(
            AuthorizationManagementDependency.KeysResponse(Seq(createdKey.copy(relationshipId = relationship1.id)))
          )
        )

      (mockAuthorizationManagementService
        .getClientKeys(_: UUID)(_: String))
        .expects(client2.id, bearerToken)
        .once()
        .returns(
          Future.successful(
            AuthorizationManagementDependency.KeysResponse(Seq(createdKey.copy(relationshipId = relationship2.id)))
          )
        )

      val expected = Seq(apiClientKey, apiClientKey)

      Get() ~> service.getOperatorKeys(personId.toString) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[ClientKeys] shouldEqual ClientKeys(expected)
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      Get() ~> service.getOperatorKeys(personId.toString) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "fail if client or key do not exist" in {

      execForEachOperatorClientExpectations()

      (mockAuthorizationManagementService
        .getClientKeys(_: UUID)(_: String))
        .expects(*, bearerToken)
        .twice()
        .returns(Future.failed(authorizationmanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.getOperatorKeys(personId.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  def execForEachOperatorClientExpectations(): Unit = {
    (mockJwtReader
      .getClaims(_: String))
      .expects(bearerToken)
      .returning(mockSubject(UUID.randomUUID().toString))
      .once()

    (mockPartyManagementService
      .getRelationshipsByPersonId(_: UUID, _: Seq[String])(_: String))
      .expects(personId, Seq.empty, bearerToken)
      .once()
      .returns(Future.successful(relationships))

    (mockAuthorizationManagementService
      .listClients(_: Option[Int], _: Option[Int], _: Option[UUID], _: Option[UUID])(_: String))
      .expects(None, None, Some(relationship1.id), None, bearerToken)
      .once()
      .returns(Future.successful(Seq(client1)))

    (mockAuthorizationManagementService
      .listClients(_: Option[Int], _: Option[Int], _: Option[UUID], _: Option[UUID])(_: String))
      .expects(None, None, Some(relationship2.id), None, bearerToken)
      .once()
      .returns(Future.successful(Seq(client2)))

    (mockAuthorizationManagementService
      .listClients(_: Option[Int], _: Option[Int], _: Option[UUID], _: Option[UUID])(_: String))
      .expects(None, None, Some(relationship3.id), None, bearerToken)
      .once()
      .returns(Future.successful(Seq()))

    ()
  }
}
