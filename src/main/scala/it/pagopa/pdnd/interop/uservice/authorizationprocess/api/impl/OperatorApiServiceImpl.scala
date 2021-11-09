package it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import cats.implicits._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.OperatorApiService
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.utils.{EitherOps, OptionOps, toUuid}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.error._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.AuthorizationManagementService.keyUseToDependency
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service._
import it.pagopa.pdnd.interop.uservice.keymanagement
import it.pagopa.pdnd.interop.uservice.keymanagement.client.invoker.{ApiError => AuthorizationManagementApiError}
import it.pagopa.pdnd.interop.uservice.partymanagement.client.model.{Problem => _, _}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

final case class OperatorApiServiceImpl(
  authorizationManagementService: AuthorizationManagementService,
  partyManagementService: PartyManagementService
)(implicit ec: ExecutionContext)
    extends OperatorApiService {

  /** Code: 201, Message: Keys created, DataType: ClientKeys
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 403, Message: Forbidden, DataType: Problem
    * Code: 404, Message: Client id not found, DataType: Problem
    */
  override def createOperatorKeys(operatorId: String, operatorKeySeeds: Seq[OperatorKeySeed])(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _            <- extractBearer(contexts)
      operatorUuid <- toUuid(operatorId).toFuture
      relationships <- partyManagementService.getRelationshipsByPersonId(
        operatorUuid,
        Some(PartyManagementService.ROLE_SECURITY_OPERATOR)
      )
      keysResponse <- operatorKeySeeds.traverse { seed =>
        for {
          clientUuid <- toUuid(seed.clientId).toFuture
          client     <- authorizationManagementService.getClient(clientUuid)
          clientRelationshipId <- client.relationships
            .intersect(relationships.items.map(_.id).toSet)
            .headOption // Exactly one expected
            .toFuture(new RuntimeException(s"ID $operatorId has no relationship with client ${seed.clientId}"))
          managementSeed = keymanagement.client.model.KeySeed(
            relationshipId = clientRelationshipId,
            key = seed.key,
            use = keyUseToDependency(seed.use),
            alg = seed.alg
          )
          result <- authorizationManagementService.createKeys(client.id, Seq(managementSeed))
        } yield result
      }

    } yield ClientKeys(keysResponse.flatMap(_.keys.map(AuthorizationManagementService.keyToApi)))

    onComplete(result) {
      case Success(keys) => createOperatorKeys201(keys)
      case Failure(ex @ UnauthenticatedError) =>
        createOperatorKeys401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: EnumParameterError) => createOperatorKeys400(Problem(Option(ex.getMessage), 400, "Bad Request"))
      case Failure(ex: SecurityOperatorRelationshipNotFound) =>
        createOperatorKeys403(Problem(Option(ex.getMessage), 403, "Forbidden"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        createOperatorKeys404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(ex) => complete(500, Problem(Option(ex.getMessage), 500, "Error on key creation"))
    }
  }

  /** Code: 204, Message: the corresponding key has been deleted.
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Key not found, DataType: Problem
    */
  override def deleteOperatorKeyById(operatorId: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _            <- extractBearer(contexts)
      operatorUuid <- toUuid(operatorId).toFuture
      _ <- collectFirstForEachOperatorClient(
        operatorUuid,
        client => authorizationManagementService.deleteKey(client.id, keyId)
      )
    } yield ()

    onComplete(result) {
      case Success(_) => deleteOperatorKeyById204
      case Failure(ex @ UnauthenticatedError) =>
        deleteOperatorKeyById401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        deleteOperatorKeyById404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(_ @NoResultsError) => deleteOperatorKeyById404(Problem(None, 404, "Not found"))
      case Failure(ex)                => complete((500, Problem(Option(ex.getMessage), 500, "Error on operator key delete")))
    }
  }

  /** Code: 200, Message: returns the corresponding key, DataType: ClientKey
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Key not found, DataType: Problem
    */
  override def getOperatorKeyById(operatorId: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientKey: ToEntityMarshaller[ClientKey],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _            <- extractBearer(contexts)
      operatorUuid <- toUuid(operatorId).toFuture
      key <- collectFirstForEachOperatorClient(
        operatorUuid,
        client => authorizationManagementService.getKey(client.id, keyId)
      )
    } yield AuthorizationManagementService.keyToApi(key)

    onComplete(result) {
      case Success(result) => getOperatorKeyById200(result)
      case Failure(ex @ UnauthenticatedError) =>
        getOperatorKeyById401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        getOperatorKeyById404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(_ @NoResultsError) => deleteOperatorKeyById404(Problem(None, 404, "Not found"))
      case Failure(ex)                => complete((500, Problem(Option(ex.getMessage), 500, "Error on key retrieve")))
    }
  }

  /** Code: 200, Message: returns the corresponding array of keys, DataType: ClientKeys
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client id not found, DataType: Problem
    */
  override def getOperatorKeys(operatorId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _            <- extractBearer(contexts)
      operatorUuid <- toUuid(operatorId).toFuture
      keysResponse <- collectAllForEachOperatorClient(
        operatorUuid,
        (client, operatorRelationships) =>
          for {
            clientKeys <- authorizationManagementService.getClientKeys(client.id)
            operatorKeys = clientKeys.keys.filter(key => operatorRelationships.items.exists(_.id == key.relationshipId))
          } yield keymanagement.client.model.KeysResponse(operatorKeys)
      )
    } yield ClientKeys(keysResponse.flatMap(_.keys.map(AuthorizationManagementService.keyToApi)))

    onComplete(result) {
      case Success(result) => getOperatorKeys200(result)
      case Failure(ex @ UnauthenticatedError) =>
        getOperatorKeys401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        getOperatorKeys404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(_ @NoResultsError) => getOperatorKeys404(Problem(None, 404, "Not found"))
      case Failure(ex)                => complete((500, Problem(Option(ex.getMessage), 500, "Error on keys retrieve")))
    }
  }

  /** Code: 200, Message: returns the corresponding array of keys, DataType: ClientKeys
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client id not found, DataType: Problem
    */
  override def getClientOperatorKeys(clientId: String, operatorId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _             <- extractBearer(contexts)
      operatorUuid  <- toUuid(operatorId).toFuture
      relationships <- partyManagementService.getRelationshipsByPersonId(operatorUuid, None)
      clientUuid    <- toUuid(clientId).toFuture
      clientKeys    <- authorizationManagementService.getClientKeys(clientUuid)
      operatorKeys = clientKeys.keys.filter(key => relationships.items.exists(_.id == key.relationshipId))
      keysResponse = keymanagement.client.model.KeysResponse(operatorKeys)
    } yield ClientKeys(keysResponse.keys.map(AuthorizationManagementService.keyToApi))

    onComplete(result) {
      case Success(result) => getClientOperatorKeys200(result)
      case Failure(ex @ UnauthenticatedError) =>
        getClientOperatorKeys401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        getClientOperatorKeys404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(_ @NoResultsError) => getClientOperatorKeys404(Problem(None, 404, "Not found"))
      case Failure(ex)                => complete((500, Problem(Option(ex.getMessage), 500, "Error on keys retrieve")))
    }
  }

  /** Exec f for each operator client, and returns the all successful operations, failure otherwise
    */
  private def collectAllForEachOperatorClient[T](
    operatorId: UUID,
    f: (ManagementClient, Relationships) => Future[T]
  ): Future[Seq[T]] = for {
    relationships <- partyManagementService.getRelationshipsByPersonId(operatorId, None)
    clients <- relationships.items.flatTraverse(relationship =>
      authorizationManagementService.listClients(
        relationshipId = Some(relationship.id),
        offset = None,
        limit = None,
        eServiceId = None,
        consumerId = None
      )
    )
    recoverable <- clients.traverse(client => f(client, relationships).transform(Success(_)))
    success      = recoverable.collect { case Success(result) => result }
    firstFailure = recoverable.collectFirst { case Failure(ex) => ex }
    result <- (success, firstFailure) match {
      case (Nil, Some(ex)) => Future.failed(ex)
      case (Nil, None)     => Future.failed(NoResultsError)
      case (result, _)     => Future.successful(result)
    }
  } yield result

  /** Exec f for each operator client, and returns the first successful operation, failure otherwise
    */
  private def collectFirstForEachOperatorClient[T](
    operatorId: UUID,
    f: (ManagementClient, Relationships) => Future[T]
  ): Future[T] = for {
    successes <- collectAllForEachOperatorClient(operatorId, f)
    result <- successes match {
      case Nil       => Future.failed(NoResultsError)
      case head +: _ => Future.successful(head)
    }
  } yield result

  private def collectFirstForEachOperatorClient[T](operatorId: UUID, f: ManagementClient => Future[T]): Future[T] =
    collectFirstForEachOperatorClient(operatorId, (client, _) => f(client))

}
