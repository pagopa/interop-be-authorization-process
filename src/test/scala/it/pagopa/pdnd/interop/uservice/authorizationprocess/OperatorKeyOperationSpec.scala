package it.pagopa.pdnd.interop.uservice.authorizationprocess

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl.OperatorApiServiceImpl
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.{ManagementClient, PartyManagementService}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.util.SpecUtils
import it.pagopa.pdnd.interop.uservice.keymanagement
import it.pagopa.pdnd.interop.uservice.partymanagement.client.model.{Relationship, RelationshipEnums, Relationships}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class OperatorKeyOperationSpec extends AnyWordSpecLike with MockFactory with SpecUtils with ScalatestRouteTest {
  import clientApiMarshaller._

  val service = new OperatorApiServiceImpl(mockAuthorizationManagementService, mockPartyManagementService)(
    ExecutionContext.global
  )

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
    from = person.taxCode,
    to = "institutionId1",
    role = RelationshipEnums.Role.Operator,
    platformRole = PartyManagementService.ROLE_SECURITY_OPERATOR,
    status = RelationshipEnums.Status.Active
  )

  val relationship2: Relationship = Relationship(
    id = UUID.randomUUID(),
    from = person.taxCode,
    to = "institutionId2",
    role = RelationshipEnums.Role.Operator,
    platformRole = PartyManagementService.ROLE_SECURITY_OPERATOR,
    status = RelationshipEnums.Status.Active
  )

  val relationship3: Relationship = Relationship(
    id = UUID.randomUUID(),
    from = person.taxCode,
    to = "institutionId3",
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
    relationships = Set(relationship1.id)
  )
  val client2: ManagementClient = keymanagement.client.model.Client(
    id = UUID.randomUUID(),
    eServiceId = UUID.randomUUID(),
    consumerId = UUID.randomUUID(),
    name = "client2",
    description = None,
    relationships = Set(relationship2.id)
  )

  "Retrieve key" should {
    "succeed" in {
      (mockPartyManagementService.getRelationshipsByTaxCode _)
        .expects(taxCode, None)
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

      (mockAuthorizationManagementService.getKey _)
        .expects(client1.id, kid)
        .once()
        .returns(Future.successful(createdKey))

      (mockAuthorizationManagementService.getKey _)
        .expects(client2.id, kid)
        .once()
        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))

      val expected = apiClientKey

      Get() ~> service.getOperatorKeyById(taxCode, kid) ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[ClientKey] shouldEqual expected
      }
    }

    "fail if missing authorization header" in {
      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
      val kid                                      = "some-kid"
      Get() ~> service.getOperatorKeyById(taxCode, kid) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

//    "fail if client or key do not exist" in {
//      val kid = "some-kid"
//      (mockAuthorizationManagementService.getKey _)
//        .expects(*, *)
//        .once()
//        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))
//
//      Get() ~> service.getClientKeyById(client.id.toString, kid) ~> check {
//        status shouldEqual StatusCodes.NotFound
//      }
//    }
  }

//  "Retrieve all client keys" should {
//    "succeed" in {
//      (mockAuthorizationManagementService.getClientKeys _)
//        .expects(client.id)
//        .once()
//        .returns(Future.successful(KeysResponse(Seq(createdKey))))
//
//      val expected = apiClientKey
//
//      Get() ~> service.getClientKeys(client.id.toString) ~> check {
//        status shouldEqual StatusCodes.OK
//        entityAs[ClientKeys] shouldEqual ClientKeys(Seq(expected))
//      }
//    }
//
//    "fail if missing authorization header" in {
//      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
//      Get() ~> service.getClientKeys(client.id.toString) ~> check {
//        status shouldEqual StatusCodes.Unauthorized
//      }
//    }
//
//    "fail if client or key do not exist" in {
//      (mockAuthorizationManagementService.getClientKeys _)
//        .expects(*)
//        .once()
//        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))
//
//      Get() ~> service.getClientKeys(client.id.toString) ~> check {
//        status shouldEqual StatusCodes.NotFound
//      }
//    }
//  }
//
//  "Create client keys" should {
//    "succeed" in {
//      val keySeeds: Seq[KeySeed] = Seq(
//        KeySeed(
//          operatorTaxCode = person.taxCode,
//          key = "key",
//          use = keymanagement.client.model.KeySeedEnums.Use.Sig.toString,
//          alg = "123"
//        )
//      )
//
//      (mockAuthorizationManagementService.getClient _)
//        .expects(client.id.toString)
//        .once()
//        .returns(Future.successful(client))
//
//      (mockPartyManagementService.getOrganization _)
//        .expects(client.consumerId)
//        .once()
//        .returns(Future.successful(organization))
//
//      (mockPartyManagementService.getRelationships _)
//        .expects(organization.institutionId, person.taxCode, PartyManagementService.ROLE_SECURITY_OPERATOR)
//        .once()
//        .returns(Future.successful(relationships))
//
//      (mockAuthorizationManagementService.createKeys _)
//        .expects(client.id, *)
//        .once()
//        .returns(Future.successful(KeysResponse(Seq(createdKey))))
//
//      val expected = apiClientKey
//
//      Get() ~> service.createKeys(client.id.toString, keySeeds) ~> check {
//        status shouldEqual StatusCodes.Created
//        entityAs[ClientKeys] shouldEqual ClientKeys(Seq(expected))
//      }
//    }
//
//    "fail on wrong enum parameters" in {
//      val keySeeds: Seq[KeySeed] =
//        Seq(KeySeed(operatorTaxCode = person.taxCode, key = "key", use = "non-existing-use-value", alg = "123"))
//
//      (mockAuthorizationManagementService.getClient _)
//        .expects(client.id.toString)
//        .once()
//        .returns(Future.successful(client))
//
//      (mockPartyManagementService.getOrganization _)
//        .expects(client.consumerId)
//        .once()
//        .returns(Future.successful(organization))
//
//      (mockPartyManagementService.getRelationships _)
//        .expects(organization.institutionId, person.taxCode, PartyManagementService.ROLE_SECURITY_OPERATOR)
//        .once()
//        .returns(Future.successful(relationships))
//
//      Get() ~> service.createKeys(client.id.toString, keySeeds) ~> check {
//        status shouldEqual StatusCodes.BadRequest
//      }
//    }
//
//    "fail if missing authorization header" in {
//      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
//      Get() ~> service.createKeys(client.id.toString, Seq.empty) ~> check {
//        status shouldEqual StatusCodes.Unauthorized
//      }
//    }
//
//    "fail if client or key do not exist" in {
//      (mockAuthorizationManagementService.getClient _)
//        .expects(client.id.toString)
//        .once()
//        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "Some message", None)))
//
//      Get() ~> service.createKeys(client.id.toString, Seq.empty) ~> check {
//        status shouldEqual StatusCodes.NotFound
//      }
//    }
//  }
//
//  "Delete key" should {
//    "succeed" in {
//      val kid = "some-kid"
//      (mockAuthorizationManagementService.deleteKey _)
//        .expects(client.id, kid)
//        .once()
//        .returns(Future.successful(()))
//
//      Get() ~> service.deleteClientKeyById(client.id.toString, kid) ~> check {
//        status shouldEqual StatusCodes.NoContent
//      }
//    }
//
//    "fail if missing authorization header" in {
//      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
//      val kid                                      = "some-kid"
//      Get() ~> service.deleteClientKeyById(client.id.toString, kid) ~> check {
//        status shouldEqual StatusCodes.Unauthorized
//      }
//    }
//
//    "fail if client or key do not exist" in {
//      val kid = "some-kid"
//      (mockAuthorizationManagementService.deleteKey _)
//        .expects(*, *)
//        .once()
//        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))
//
//      Get() ~> service.deleteClientKeyById(client.id.toString, kid) ~> check {
//        status shouldEqual StatusCodes.NotFound
//      }
//    }
//  }
//
//  "Enable key" should {
//    "succeed" in {
//      val kid = "some-kid"
//      (mockAuthorizationManagementService.enableKey _)
//        .expects(client.id, kid)
//        .once()
//        .returns(Future.successful(()))
//
//      Get() ~> service.enableKeyById(client.id.toString, kid) ~> check {
//        status shouldEqual StatusCodes.NoContent
//      }
//    }
//
//    "fail if missing authorization header" in {
//      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
//      val kid                                      = "some-kid"
//      Get() ~> service.enableKeyById(client.id.toString, kid) ~> check {
//        status shouldEqual StatusCodes.Unauthorized
//      }
//    }
//
//    "fail if client or key do not exist" in {
//      val kid = "some-kid"
//      (mockAuthorizationManagementService.enableKey _)
//        .expects(*, *)
//        .once()
//        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))
//
//      Get() ~> service.enableKeyById(client.id.toString, kid) ~> check {
//        status shouldEqual StatusCodes.NotFound
//      }
//    }
//  }
//
//  "Disable key" should {
//    "succeed" in {
//      val kid = "some-kid"
//      (mockAuthorizationManagementService.disableKey _)
//        .expects(client.id, kid)
//        .once()
//        .returns(Future.successful(()))
//
//      Get() ~> service.disableKeyById(client.id.toString, kid) ~> check {
//        status shouldEqual StatusCodes.NoContent
//      }
//    }
//
//    "fail if missing authorization header" in {
//      implicit val contexts: Seq[(String, String)] = Seq.empty[(String, String)]
//      val kid                                      = "some-kid"
//      Get() ~> service.disableKeyById(client.id.toString, kid) ~> check {
//        status shouldEqual StatusCodes.Unauthorized
//      }
//    }
//
//    "fail if client or key do not exist" in {
//      val kid = "some-kid"
//      (mockAuthorizationManagementService.disableKey _)
//        .expects(*, *)
//        .once()
//        .returns(Future.failed(keymanagement.client.invoker.ApiError(404, "message", None)))
//
//      Get() ~> service.disableKeyById(client.id.toString, kid) ~> check {
//        status shouldEqual StatusCodes.NotFound
//      }
//    }
//  }
}
