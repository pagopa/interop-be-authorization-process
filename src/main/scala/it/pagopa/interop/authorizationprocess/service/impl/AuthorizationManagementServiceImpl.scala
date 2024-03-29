package it.pagopa.interop.authorizationprocess.service.impl

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.authorizationmanagement.client.api.{ClientApi, KeyApi, PurposeApi}
import it.pagopa.interop.authorizationmanagement.client.invoker.{ApiError, ApiRequest, BearerToken}
import it.pagopa.interop.authorizationmanagement.client.model._
import it.pagopa.interop.authorizationmanagement.model.client.{PersistentClient, PersistentClientKind}
import it.pagopa.interop.authorizationmanagement.model.key.PersistentKey
import it.pagopa.interop.authorizationprocess.common.readmodel.PaginatedResult
import it.pagopa.interop.authorizationprocess.common.readmodel.model.ReadModelClientWithKeys
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors._
import it.pagopa.interop.authorizationprocess.service.{AuthorizationManagementInvoker, AuthorizationManagementService}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.withHeaders
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.authorizationprocess.common.readmodel.ReadModelAuthorizationQueries
import cats.syntax.all._

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

final case class AuthorizationManagementServiceImpl(
  invoker: AuthorizationManagementInvoker,
  clientApi: ClientApi,
  keyApi: KeyApi,
  purposeApi: PurposeApi
)(implicit ec: ExecutionContext)
    extends AuthorizationManagementService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog]                                                 =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)
  override def createClient(
    consumerId: UUID,
    name: String,
    description: Option[String],
    kind: ClientKind,
    createdAt: OffsetDateTime,
    users: Seq[UUID]
  )(implicit contexts: Seq[(String, String)]): Future[Client] = withHeaders[Client] { (bearerToken, correlationId) =>
    val request: ApiRequest[Client] = clientApi.createClient(
      xCorrelationId = correlationId,
      ClientSeed(
        consumerId = consumerId,
        name = name,
        description = description,
        kind = kind,
        createdAt = createdAt,
        users = users
      )
    )(BearerToken(bearerToken))
    invoker.invoke(request, "Client creation")
  }
  override def getClient(
    clientId: UUID
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PersistentClient] =
    ReadModelAuthorizationQueries.getClientById(clientId).flatMap(_.toFuture(ClientNotFound(clientId)))
  override def deleteClient(clientId: UUID)(implicit contexts: Seq[(String, String)]): Future[Unit]             =
    withHeaders[Unit] { (bearerToken, correlationId) =>
      val request: ApiRequest[Unit] =
        clientApi.deleteClient(xCorrelationId = correlationId, clientId.toString)(BearerToken(bearerToken))
      invoker
        .invoke(request, "Client delete")
        .recoverWith {
          case err: ApiError[_] if err.code == 404 => Future.failed(ClientNotFound(clientId))
        }
    }
  override def addUser(clientId: UUID, userId: UUID)(implicit contexts: Seq[(String, String)]): Future[Client]  =
    withHeaders[Client] { (bearerToken, correlationId) =>
      val request: ApiRequest[Client] =
        clientApi.addUser(xCorrelationId = correlationId, clientId, UserSeed(userId))(BearerToken(bearerToken))
      invoker.invoke(request, "User addition to client")
    }
  override def removeUser(clientId: UUID, userId: UUID)(implicit contexts: Seq[(String, String)]): Future[Unit] =
    withHeaders[Unit] { (bearerToken, correlationId) =>
      val request: ApiRequest[Unit] =
        clientApi.removeClientUser(xCorrelationId = correlationId, clientId, userId)(BearerToken(bearerToken))
      invoker
        .invoke(request, "User removal from client")
        .recoverWith {
          case err: ApiError[_] if err.code == 404 => Future.failed(ClientUserNotFound(clientId, userId))
        }
    }
  override def getClientKey(clientId: UUID, kid: String)(implicit
    ec: ExecutionContext,
    readModel: ReadModelService
  ): Future[PersistentKey] = for {
    keys <- ReadModelAuthorizationQueries
      .getClientKey(clientId, kid.some)
      .flatMap(_.map(_.keys).toFuture(ClientKeyNotFound(clientId, kid)))
    key  <- keys.find(_.kid == kid).toFuture(ClientKeyNotFound(clientId, kid))
  } yield key
  override def deleteKey(clientId: UUID, kid: String)(implicit contexts: Seq[(String, String)]): Future[Unit]   =
    withHeaders[Unit] { (bearerToken, correlationId) =>
      val request: ApiRequest[Unit] =
        keyApi.deleteClientKeyById(xCorrelationId = correlationId, clientId, kid)(BearerToken(bearerToken))
      invoker
        .invoke(request, "Key Delete")
        .recoverWith {
          case err: ApiError[_] if err.code == 404 => Future.failed(ClientKeyNotFound(clientId, kid))
        }
    }
  override def getClientKeys(
    clientId: UUID
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[Seq[PersistentKey]] = {
    ReadModelAuthorizationQueries.getClientKeys(clientId).flatMap(_.map(_.keys).toFuture(ClientNotFound(clientId)))
  }
  override def createKeys(clientId: UUID, keysSeeds: Seq[KeySeed])(implicit
    contexts: Seq[(String, String)]
  ): Future[Keys] = withHeaders[Keys] { (bearerToken, correlationId) =>
    val request: ApiRequest[Keys] =
      keyApi.createKeys(xCorrelationId = correlationId, clientId, keysSeeds)(BearerToken(bearerToken))
    invoker
      .invoke(request, "Key creation")
      .recoverWith {
        case err: ApiError[_] if err.code == 400 => Future.failed(CreateKeysBadRequest(err.message))
        case err: ApiError[_] if err.code == 409 => Future.failed(KeysAlreadyExist(err.message))
      }
  }
  override def addClientPurpose(clientId: UUID, purposeSeed: PurposeSeed)(implicit
    contexts: Seq[(String, String)]
  ): Future[Purpose] = withHeaders[Purpose] { (bearerToken, correlationId) =>
    val request: ApiRequest[Purpose] =
      purposeApi.addClientPurpose(xCorrelationId = correlationId, clientId, purposeSeed)(BearerToken(bearerToken))
    invoker
      .invoke(request, "Purpose addition to client")
      .recoverWith {
        case err: ApiError[_] if err.code == 404 => Future.failed(ClientNotFound(clientId))
      }
  }
  override def removeClientPurpose(clientId: UUID, purposeId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[Unit] = withHeaders[Unit] { (bearerToken, correlationId) =>
    val request: ApiRequest[Unit] =
      purposeApi.removeClientPurpose(xCorrelationId = correlationId, clientId, purposeId)(BearerToken(bearerToken))
    invoker
      .invoke(request, "Purpose remove from client")
      .recoverWith {
        case err: ApiError[_] if err.code == 404 => Future.failed(ClientNotFound(clientId))
      }
  }

  override def getClientsByPurpose(
    purposeId: UUID
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[Seq[PersistentClient]] =
    ReadModelAuthorizationQueries.getClientsByPurpose(purposeId)

  override def getClientsWithKeys(
    name: Option[String],
    userIds: List[UUID],
    consumerId: UUID,
    purposeId: Option[UUID],
    kind: Option[PersistentClientKind],
    offset: Int,
    limit: Int
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PaginatedResult[ReadModelClientWithKeys]] =
    ReadModelAuthorizationQueries.getClientsWithKeys(name, userIds, consumerId, purposeId, kind, offset, limit)

  override def getClients(
    name: Option[String],
    userIds: List[UUID],
    consumerId: UUID,
    purposeId: Option[UUID],
    kind: Option[PersistentClientKind],
    offset: Int,
    limit: Int
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PaginatedResult[PersistentClient]] =
    ReadModelAuthorizationQueries.getClients(name, userIds, consumerId, purposeId, kind, offset, limit)
}
