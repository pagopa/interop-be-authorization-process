package it.pagopa.interop.authorizationprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import cats.implicits._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.authorizationmanagement
import it.pagopa.interop.authorizationmanagement.client.invoker.{ApiError => AuthorizationManagementApiError}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.TypeConversions.{OptionOps, StringOps}
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.{MissingBearer, ResourceNotFoundError}
import it.pagopa.interop.authorizationprocess.api.OperatorApiService
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors._
import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationprocess.service.AuthorizationManagementService.keyUseToDependency
import it.pagopa.interop.authorizationprocess.service._
import it.pagopa.interop.commons.utils.AkkaUtils.getFutureBearer
import it.pagopa.interop.partymanagement.client.model.{Problem => _, _}
import org.slf4j.LoggerFactory

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

final case class OperatorApiServiceImpl(
  authorizationManagementService: AuthorizationManagementService,
  partyManagementService: PartyManagementService
)(implicit ec: ExecutionContext)
    extends OperatorApiService {

  val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](LoggerFactory.getLogger(this.getClass))

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
    logger.info("Creating operator keys {}", operatorId)
    val result = for {
      bearerToken  <- getFutureBearer(contexts)
      operatorUuid <- operatorId.toFutureUUID
      relationships <- partyManagementService.getRelationshipsByPersonId(
        operatorUuid,
        Seq(PartyManagementService.PRODUCT_ROLE_SECURITY_OPERATOR)
      )(bearerToken)
      keysResponse <- operatorKeySeeds.traverse { seed =>
        for {
          clientUuid <- seed.clientId.toFutureUUID
          client     <- authorizationManagementService.getClient(clientUuid)(bearerToken)
          clientRelationshipId <- client.relationships
            .intersect(relationships.items.map(_.id).toSet)
            .headOption // Exactly one expected
            .toFuture(new RuntimeException(s"ID $operatorId has no relationship with client ${seed.clientId}"))
          managementSeed = authorizationmanagement.client.model.KeySeed(
            relationshipId = clientRelationshipId,
            key = seed.key,
            use = keyUseToDependency(seed.use),
            alg = seed.alg,
            name = seed.name
          )
          result <- authorizationManagementService.createKeys(client.id, Seq(managementSeed))(bearerToken)
        } yield result
      }

    } yield ClientKeys(keysResponse.flatMap(_.keys.map(AuthorizationManagementService.keyToApi)))

    onComplete(result) {
      case Success(keys) => createOperatorKeys201(keys)
      case Failure(MissingBearer) =>
        logger.error(s"Error while creating operator keys ${operatorId} - ${MissingBearer.getMessage}")
        createOperatorKeys401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: EnumParameterError) =>
        logger.error(s"Error while creating operator keys ${operatorId} - ${ex.getMessage}")
        createOperatorKeys400(problemOf(StatusCodes.BadRequest, ex))
      case Failure(ex: SecurityOperatorRelationshipNotFound) =>
        logger.error(s"Error while creating operator keys ${operatorId} - ${ex.getMessage}")
        createOperatorKeys403(problemOf(StatusCodes.Forbidden, ex))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error(s"Error while creating operator keys ${operatorId} - ${ex.getMessage}")
        createOperatorKeys404(problemOf(StatusCodes.NotFound, ResourceNotFoundError(operatorId)))
      case Failure(ex) =>
        logger.error(s"Error while creating operator keys ${operatorId} - ${ex.getMessage}")
        val error = problemOf(StatusCodes.InternalServerError, OperatorKeyCreationError)
        complete(error.status, error)
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
    logger.info("Deleting operator {} key {}", operatorId, keyId)
    val result = for {
      bearerToken  <- getFutureBearer(contexts)
      operatorUuid <- operatorId.toFutureUUID
      _ <- collectFirstForEachOperatorClient(
        operatorUuid,
        client => authorizationManagementService.deleteKey(client.id, keyId)(bearerToken)
      )(bearerToken)
    } yield ()

    onComplete(result) {
      case Success(_) => deleteOperatorKeyById204
      case Failure(MissingBearer) =>
        logger.error(s"Error while deleting operator ${operatorId} key ${keyId} - ${MissingBearer.getMessage}")
        deleteOperatorKeyById401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error(s"Error while deleting operator ${operatorId} key ${keyId} - ${ex.getMessage}")
        deleteOperatorKeyById404(
          problemOf(StatusCodes.NotFound, ResourceNotFoundError(s"operator id: $operatorId, key id: $keyId"))
        )
      case Failure(NoResultsError) =>
        logger.error(s"Error while deleting operator ${operatorId} key ${keyId} - ${NoResultsError.getMessage}")
        deleteOperatorKeyById404(problemOf(StatusCodes.NotFound, NoResultsError))
      case Failure(ex) =>
        logger.error(s"Error while deleting operator ${operatorId} key ${keyId} - ${ex.getMessage}")
        val error = problemOf(StatusCodes.InternalServerError, OperatorKeyDeletionError)
        complete((error.status, error))
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
    logger.info("Getting operator {} key {}", operatorId, keyId)
    val result = for {
      bearerToken  <- getFutureBearer(contexts)
      operatorUuid <- operatorId.toFutureUUID
      key <- collectFirstForEachOperatorClient(
        operatorUuid,
        client => authorizationManagementService.getKey(client.id, keyId)(bearerToken)
      )(bearerToken)
    } yield AuthorizationManagementService.keyToApi(key)

    onComplete(result) {
      case Success(result) => getOperatorKeyById200(result)
      case Failure(MissingBearer) =>
        logger.error(s"Error while getting operator ${operatorId} key ${keyId} - ${MissingBearer.getMessage}")
        getOperatorKeyById401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error(s"Error while getting operator ${operatorId} key ${keyId} - ${ex.getMessage}")
        getOperatorKeyById404(
          problemOf(StatusCodes.NotFound, ResourceNotFoundError(s"operator id: $operatorId, key id: $keyId"))
        )
      case Failure(NoResultsError) =>
        logger.error(s"Error while getting operator ${operatorId} key ${keyId} - ${NoResultsError.getMessage}")
        deleteOperatorKeyById404(problemOf(StatusCodes.NotFound, NoResultsError))
      case Failure(ex) =>
        logger.error(s"Error while getting operator ${operatorId} key ${keyId} - ${ex.getMessage}")
        val error = problemOf(StatusCodes.InternalServerError, OperatorKeyRetrievalError)
        complete((error.status, error))
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
    logger.info("Getting operator {} keys", operatorId)
    val result = for {
      bearerToken  <- getFutureBearer(contexts)
      operatorUuid <- operatorId.toFutureUUID
      keysResponse <- collectAllForEachOperatorClient(
        operatorUuid,
        (client, operatorRelationships) =>
          for {
            clientKeys <- authorizationManagementService.getClientKeys(client.id)(bearerToken)
            operatorKeys = clientKeys.keys.filter(key => operatorRelationships.items.exists(_.id == key.relationshipId))
          } yield authorizationmanagement.client.model.KeysResponse(operatorKeys)
      )(bearerToken)
    } yield ClientKeys(keysResponse.flatMap(_.keys.map(AuthorizationManagementService.keyToApi)))

    onComplete(result) {
      case Success(result) => getOperatorKeys200(result)
      case Failure(MissingBearer) =>
        logger.error(s"Error while getting operator ${operatorId} keys - ${MissingBearer.getMessage}")
        getOperatorKeys401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error(s"Error while getting operator ${operatorId} keys - ${ex.getMessage}")
        getOperatorKeys404(problemOf(StatusCodes.NotFound, ResourceNotFoundError(operatorId)))
      case Failure(NoResultsError) =>
        logger.error(s"Error while getting operator ${operatorId} keys - ${NoResultsError.getMessage}")
        getOperatorKeys404(problemOf(StatusCodes.NotFound, NoResultsError))
      case Failure(ex) =>
        logger.error(s"Error while getting operator ${operatorId} keys - ${ex.getMessage}")
        val error = problemOf(StatusCodes.InternalServerError, OperatorKeyRetrievalError)
        complete((error.status, error))
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
    logger.info("Getting client keys {} for operator {}", clientId, operatorId)
    val result = for {
      bearerToken   <- getFutureBearer(contexts)
      operatorUuid  <- operatorId.toFutureUUID
      relationships <- partyManagementService.getRelationshipsByPersonId(operatorUuid, Seq.empty)(bearerToken)
      clientUuid    <- clientId.toFutureUUID
      clientKeys    <- authorizationManagementService.getClientKeys(clientUuid)(bearerToken)
      operatorKeys = clientKeys.keys.filter(key => relationships.items.exists(_.id == key.relationshipId))
      keysResponse = authorizationmanagement.client.model.KeysResponse(operatorKeys)
    } yield ClientKeys(keysResponse.keys.map(AuthorizationManagementService.keyToApi))

    onComplete(result) {
      case Success(result) => getClientOperatorKeys200(result)
      case Failure(MissingBearer) =>
        logger.error(
          s"Error while getting client ${clientId} keys for operator ${operatorId} keys - ${MissingBearer.getMessage}"
        )
        getClientOperatorKeys401(problemOf(StatusCodes.Unauthorized, MissingBearer))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        logger.error(s"Error while getting client ${clientId} keys for operator ${operatorId} keys - ${ex.getMessage}")
        getClientOperatorKeys404(
          problemOf(StatusCodes.NotFound, ResourceNotFoundError(s"client id: $clientId, operator id: $operatorId"))
        )
      case Failure(NoResultsError) =>
        logger.error(
          s"Error while getting client ${clientId} keys for operator ${operatorId} keys - ${NoResultsError.getMessage}"
        )
        getClientOperatorKeys404(problemOf(StatusCodes.NotFound, NoResultsError))
      case Failure(ex) =>
        logger.error(s"Error while getting client ${clientId} keys for operator ${operatorId} keys - ${ex.getMessage}")
        val error = problemOf(StatusCodes.InternalServerError, OperatorKeysRetrievalError)
        complete((error.status, error))
    }
  }

  /** Exec f for each operator client, and returns the all successful operations, failure otherwise
    */
  private def collectAllForEachOperatorClient[T](operatorId: UUID, f: (ManagementClient, Relationships) => Future[T])(
    bearerToken: String
  ): Future[Seq[T]] = for {
    relationships <- partyManagementService.getRelationshipsByPersonId(operatorId, Seq.empty)(bearerToken)
    clients <- relationships.items.flatTraverse(relationship =>
      authorizationManagementService.listClients(
        relationshipId = Some(relationship.id),
        offset = None,
        limit = None,
        consumerId = None,
        purposeId = None,
        kind = None
      )(bearerToken)
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
  private def collectFirstForEachOperatorClient[T](operatorId: UUID, f: (ManagementClient, Relationships) => Future[T])(
    bearerToken: String
  ): Future[T] = for {
    successes <- collectAllForEachOperatorClient(operatorId, f)(bearerToken)
    result <- successes match {
      case Nil       => Future.failed(NoResultsError)
      case head +: _ => Future.successful(head)
    }
  } yield result

  private def collectFirstForEachOperatorClient[T](operatorId: UUID, f: ManagementClient => Future[T])(
    bearerToken: String
  ): Future[T] =
    collectFirstForEachOperatorClient(operatorId, (client, _) => f(client))(bearerToken)

}
