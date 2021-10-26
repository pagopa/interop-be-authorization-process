package it.pagopa.pdnd.interop.uservice.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.OperatorApiServiceImpl
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{ManagementClient, PartyManagementService}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.util.SpecUtils
import it.pagopa.pdnd.interop.uservice.keymanagement
import it.pagopa.pdnd.interop.uservice.keymanagement.client.model.{ClientEnums, KeysResponse}
import it.pagopa.pdnd.interop.uservice.partymanagement.client.model.{Relationship, RelationshipEnums, Relationships}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class OperatorKeyOperationSpec extends AnyWordSpecLike with MockFactory with SpecUtils with ScalatestRouteTest {
  import clientApiMarshaller._

  val service: OperatorApiServiceImpl =
    OperatorApiServiceImpl(mockAuthorizationManagementService, mockPartyManagementService)(ExecutionContext.global)

  val kid: String = "some-kid"

  val apiClientKey: ClientKey = ClientKey(
    status = createdKey.status.toString,
    key = Key(
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

  val relationship1: Relationship = Relationship(
    id = UUID.randomUUID(),
    from = user.id,
    to = UUID.randomUUID(),
    role = RelationshipEnums.Role.Operator,
    platformRole = PartyManagementService.ROLE_SECURITY_OPERATOR,
    status = RelationshipEnums.Status.Active
  )

  val relationship2: Relationship = Relationship(
    id = UUID.randomUUID(),
    from = user.id,
    to = UUID.randomUUID(),
    role = RelationshipEnums.Role.Operator,
    platformRole = PartyManagementService.ROLE_SECURITY_OPERATOR,
    status = RelationshipEnums.Status.Active
  )

  val relationship3: Relationship = Relationship(
    id = UUID.randomUUID(),
    from = user.id,
    to = UUID.randomUUID(),
    role = RelationshipEnums.Role.Manager,
    platformRole = "admin",
    status = RelationshipEnums.Status.Active
  )

  override val relationships: Relationships = Relationships(Seq(relationship1, relationship2, relationship3))

  val client1: ManagementClient = keymanagement.client.model.Client(
    id = UUID.randomUUID(),
    eServiceId = UUID.randomUUID(),
    consumerId = UUID.randomUUID(),
    name = "client1",
    description = None,
    relationships = Set(relationship1.id),
    purposes = "purpose1",
    status = ClientEnums.Status.Active
  )
  val client2: ManagementClient = keymanagement.client.model.Client(
    id = UUID.randomUUID(),
    eServiceId = UUID.randomUUID(),
    consumerId = UUID.randomUUID(),
    name = "client2",
    description = None,
    relationships = Set(relationship2.id),
    purposes = "purpose2",
    status = ClientEnums.Status.Active
  )

  "Retrieve key" should {
    "succeed" in {

      execForEachOperatorClientExpectations()

      (mockAuthorizationManagementService.getKey _)
        .expects(client1.id, kid)
        .once()
        .returns(Future.successful(createdKey))

      (mockAuthorizationManagementService.getKey _)
        .expects(client2.id, kid)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))

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

      (mockAuthorizationManagementService.getKey _)
        .expects(client1.id, kid)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))

      (mockAuthorizationManagementService.getKey _)
        .expects(client2.id, kid)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.getOperatorKeyById(personId.toString, kid) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  "Retrieve all client keys" should {
    "succeed" in {

      execForEachOperatorClientExpectations()

      (mockAuthorizationManagementService.getClientKeys _)
        .expects(client1.id)
        .once()
        .returns(Future.successful(KeysResponse(Seq(createdKey.copy(relationshipId = relationship1.id)))))

      (mockAuthorizationManagementService.getClientKeys _)
        .expects(client2.id)
        .once()
        .returns(Future.successful(KeysResponse(Seq(createdKey.copy(relationshipId = relationship2.id)))))

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

      (mockAuthorizationManagementService.getClientKeys _)
        .expects(*)
        .twice()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))

      Get() ~> service.getOperatorKeys(personId.toString) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }

  def execForEachOperatorClientExpectations(): Unit = {
    (mockPartyManagementService.getRelationshipsByPersonId _)
      .expects(personId, None)
      .once()
      .returns(Future.successful(relationships))

    (mockAuthorizationManagementService.listClients _)
      .expects(None, None, None, Some(relationship1.id), None)
      .once()
      .returns(Future.successful(Seq(client1)))

    (mockAuthorizationManagementService.listClients _)
      .expects(None, None, None, Some(relationship2.id), None)
      .once()
      .returns(Future.successful(Seq(client2)))

    (mockAuthorizationManagementService.listClients _)
      .expects(None, None, None, Some(relationship3.id), None)
      .once()
      .returns(Future.successful(Seq()))

    ()
  }
}
