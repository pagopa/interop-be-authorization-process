package it.pagopa.interop.authorizationprocess.service.impl

import it.pagopa.interop.authorizationmanagement.client.api.{ClientApi, KeyApi, PurposeApi}
import it.pagopa.interop.authorizationmanagement.client.invoker.{ApiError, ApiRequest, BearerToken}
import it.pagopa.interop.authorizationmanagement.client.model._
import it.pagopa.interop.commons.utils.withHeaders
import it.pagopa.interop.authorizationprocess.service.{AuthorizationManagementInvoker, AuthorizationManagementService}
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors.{
  ClientKeyNotFound,
  ClientNotFound,
  ClientRelationshipNotFound
}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

final case class AuthorizationManagementServiceImpl(
  invoker: AuthorizationManagementInvoker,
  clientApi: ClientApi,
  keyApi: KeyApi,
  purposeApi: PurposeApi
)(implicit ec: ExecutionContext)
    extends AuthorizationManagementService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def createClient(consumerId: UUID, name: String, description: Option[String], kind: ClientKind)(implicit
    contexts: Seq[(String, String)]
  ): Future[Client] = withHeaders[Client] { (bearerToken, correlationId, ip) =>
    val request: ApiRequest[Client] = clientApi.createClient(
      xCorrelationId = correlationId,
      ClientSeed(consumerId = consumerId, name = name, description = description, kind = kind),
      xForwardedFor = ip
    )(BearerToken(bearerToken))
    invoker.invoke(request, "Client creation")
  }

  override def getClient(clientId: UUID)(implicit contexts: Seq[(String, String)]): Future[Client] =
    withHeaders[Client] { (bearerToken, correlationId, ip) =>
      val request: ApiRequest[Client] =
        clientApi.getClient(xCorrelationId = correlationId, clientId, xForwardedFor = ip)(BearerToken(bearerToken))
      invoker
        .invoke(request, "Client retrieve")
        .recoverWith {
          case err: ApiError[_] if err.code == 404 => Future.failed(ClientNotFound(clientId))
        }
    }

  override def listClients(
    offset: Option[Int],
    limit: Option[Int],
    relationshipId: Option[UUID],
    consumerId: Option[UUID],
    purposeId: Option[UUID],
    kind: Option[ClientKind] = None
  )(implicit contexts: Seq[(String, String)]): Future[Seq[Client]] =
    withHeaders[Seq[Client]] { (bearerToken, correlationId, ip) =>
      val request: ApiRequest[Seq[Client]] = clientApi.listClients(
        xCorrelationId = correlationId,
        xForwardedFor = ip,
        offset = offset,
        limit = limit,
        relationshipId = relationshipId,
        consumerId = consumerId,
        purposeId = purposeId,
        kind = kind
      )(BearerToken(bearerToken))
      invoker.invoke(request, "Client list")
    }

  override def deleteClient(clientId: UUID)(implicit contexts: Seq[(String, String)]): Future[Unit] =
    withHeaders[Unit] { (bearerToken, correlationId, ip) =>
      val request: ApiRequest[Unit] =
        clientApi.deleteClient(xCorrelationId = correlationId, clientId.toString, xForwardedFor = ip)(
          BearerToken(bearerToken)
        )
      invoker
        .invoke(request, "Client delete")
        .recoverWith {
          case err: ApiError[_] if err.code == 404 => Future.failed(ClientNotFound(clientId))
        }
    }

  override def addRelationship(clientId: UUID, relationshipId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[Client] = withHeaders[Client] { (bearerToken, correlationId, ip) =>
    val request: ApiRequest[Client] = clientApi.addRelationship(
      xCorrelationId = correlationId,
      clientId,
      PartyRelationshipSeed(relationshipId),
      xForwardedFor = ip
    )(BearerToken(bearerToken))
    invoker.invoke(request, "Operator addition to client")
  }

  override def removeClientRelationship(clientId: UUID, relationshipId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[Unit] = withHeaders[Unit] { (bearerToken, correlationId, ip) =>
    val request: ApiRequest[Unit] =
      clientApi.removeClientRelationship(xCorrelationId = correlationId, clientId, relationshipId, xForwardedFor = ip)(
        BearerToken(bearerToken)
      )
    invoker
      .invoke(request, "Operator removal from client")
      .recoverWith {
        case err: ApiError[_] if err.code == 404 => Future.failed(ClientRelationshipNotFound(clientId, relationshipId))
      }
  }

  override def getKey(clientId: UUID, kid: String)(implicit contexts: Seq[(String, String)]): Future[ClientKey] =
    withHeaders[ClientKey] { (bearerToken, correlationId, ip) =>
      val request: ApiRequest[ClientKey] =
        keyApi.getClientKeyById(xCorrelationId = correlationId, clientId, kid, xForwardedFor = ip)(
          BearerToken(bearerToken)
        )
      invoker
        .invoke(request, "Key Retrieve")
        .recoverWith {
          case err: ApiError[_] if err.code == 404 => Future.failed(ClientKeyNotFound(clientId, kid))
        }
    }

  override def deleteKey(clientId: UUID, kid: String)(implicit contexts: Seq[(String, String)]): Future[Unit] =
    withHeaders[Unit] { (bearerToken, correlationId, ip) =>
      val request: ApiRequest[Unit] =
        keyApi.deleteClientKeyById(xCorrelationId = correlationId, clientId, kid, xForwardedFor = ip)(
          BearerToken(bearerToken)
        )
      invoker
        .invoke(request, "Key Delete")
        .recoverWith {
          case err: ApiError[_] if err.code == 404 => Future.failed(ClientKeyNotFound(clientId, kid))
        }
    }

  override def getClientKeys(clientId: UUID)(implicit contexts: Seq[(String, String)]): Future[KeysResponse] =
    withHeaders[KeysResponse] { (bearerToken, correlationId, ip) =>
      val request: ApiRequest[KeysResponse] =
        keyApi.getClientKeys(xCorrelationId = correlationId, clientId, xForwardedFor = ip)(BearerToken(bearerToken))
      invoker
        .invoke(request, "Client keys retrieve")
        .recoverWith {
          case err: ApiError[_] if err.code == 404 => Future.failed(ClientNotFound(clientId))
        }
    }

  def getEncodedClientKey(clientId: UUID, kid: String)(implicit
    contexts: Seq[(String, String)]
  ): Future[EncodedClientKey] = withHeaders[EncodedClientKey] { (bearerToken, correlationId, ip) =>
    val request: ApiRequest[EncodedClientKey] =
      keyApi.getEncodedClientKeyById(xCorrelationId = correlationId, clientId, kid, xForwardedFor = ip)(
        BearerToken(bearerToken)
      )
    invoker
      .invoke(request, "Key Retrieve")
      .recoverWith {
        case err: ApiError[_] if err.code == 404 => Future.failed(ClientKeyNotFound(clientId, kid))
      }
  }

  override def createKeys(clientId: UUID, keysSeeds: Seq[KeySeed])(implicit
    contexts: Seq[(String, String)]
  ): Future[KeysResponse] = withHeaders[KeysResponse] { (bearerToken, correlationId, ip) =>
    val request: ApiRequest[KeysResponse] =
      keyApi.createKeys(xCorrelationId = correlationId, clientId, keysSeeds, xForwardedFor = ip)(
        BearerToken(bearerToken)
      )
    invoker
      .invoke(request, "Key creation")
      .recoverWith {
        case err: ApiError[_] if err.code == 404 => Future.failed(ClientNotFound(clientId))
      }
  }

  override def addClientPurpose(clientId: UUID, purposeSeed: PurposeSeed)(implicit
    contexts: Seq[(String, String)]
  ): Future[Purpose] = withHeaders[Purpose] { (bearerToken, correlationId, ip) =>
    val request: ApiRequest[Purpose] =
      purposeApi.addClientPurpose(xCorrelationId = correlationId, clientId, purposeSeed, xForwardedFor = ip)(
        BearerToken(bearerToken)
      )
    invoker
      .invoke(request, "Purpose addition to client")
      .recoverWith {
        case err: ApiError[_] if err.code == 404 => Future.failed(ClientNotFound(clientId))
      }
  }

  override def removeClientPurpose(clientId: UUID, purposeId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[Unit] = withHeaders[Unit] { (bearerToken, correlationId, ip) =>
    val request: ApiRequest[Unit] =
      purposeApi.removeClientPurpose(xCorrelationId = correlationId, clientId, purposeId, xForwardedFor = ip)(
        BearerToken(bearerToken)
      )
    invoker
      .invoke(request, "Purpose remove from client")
      .recoverWith {
        case err: ApiError[_] if err.code == 404 => Future.failed(ClientNotFound(clientId))
      }
  }
}
