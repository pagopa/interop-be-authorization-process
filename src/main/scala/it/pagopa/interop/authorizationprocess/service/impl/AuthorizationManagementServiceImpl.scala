package it.pagopa.interop.authorizationprocess.service.impl

import it.pagopa.interop.authorizationmanagement.client.api.{ClientApi, KeyApi, PurposeApi}
import it.pagopa.interop.authorizationmanagement.client.invoker.BearerToken
import it.pagopa.interop.authorizationmanagement.client.model._
import it.pagopa.interop.authorizationprocess.service.{AuthorizationManagementInvoker, AuthorizationManagementService}
import it.pagopa.interop.commons.utils.TypeConversions.EitherOps
import it.pagopa.interop.commons.utils.extractHeaders
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
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
  ): Future[Client] = {

    for {
      (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
      request = clientApi.createClient(
        xCorrelationId = correlationId,
        ClientSeed(consumerId = consumerId, name = name, description = description, kind = kind),
        xForwardedFor = ip
      )(BearerToken(bearerToken))
      result <- invoker.invoke(request, "Client creation")
    } yield result

  }

  override def getClient(clientId: UUID)(implicit contexts: Seq[(String, String)]): Future[Client] = {
    for {
      (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
      request = clientApi.getClient(xCorrelationId = correlationId, clientId, xForwardedFor = ip)(
        BearerToken(bearerToken)
      )
      result <- invoker.invoke(request, "Client retrieve")
    } yield result
  }

  override def listClients(
    offset: Option[Int],
    limit: Option[Int],
    relationshipId: Option[UUID],
    consumerId: Option[UUID],
    purposeId: Option[UUID],
    kind: Option[ClientKind] = None
  )(implicit contexts: Seq[(String, String)]): Future[Seq[Client]] = {
    for {
      (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
      request = clientApi.listClients(
        xCorrelationId = correlationId,
        xForwardedFor = ip,
        offset = offset,
        limit = limit,
        relationshipId = relationshipId,
        consumerId = consumerId,
        purposeId = purposeId,
        kind = kind
      )(BearerToken(bearerToken))
      result <- invoker.invoke(request, "Client list")
    } yield result
  }

  override def deleteClient(clientId: UUID)(implicit contexts: Seq[(String, String)]): Future[Unit] = {
    for {
      (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
      request = clientApi.deleteClient(xCorrelationId = correlationId, clientId.toString, xForwardedFor = ip)(
        BearerToken(bearerToken)
      )
      result <- invoker.invoke(request, "Client delete")
    } yield result
  }

  override def addRelationship(clientId: UUID, relationshipId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[Client] = {
    for {
      (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
      request = clientApi.addRelationship(
        xCorrelationId = correlationId,
        clientId,
        PartyRelationshipSeed(relationshipId),
        xForwardedFor = ip
      )(BearerToken(bearerToken))
      result <- invoker.invoke(request, "Operator addition to client")
    } yield result
  }

  override def removeClientRelationship(clientId: UUID, relationshipId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[Unit] = {
    for {
      (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
      request = clientApi.removeClientRelationship(
        xCorrelationId = correlationId,
        clientId,
        relationshipId,
        xForwardedFor = ip
      )(BearerToken(bearerToken))
      result <- invoker.invoke(request, "Operator removal from client")
    } yield result
  }

  override def getKey(clientId: UUID, kid: String)(implicit contexts: Seq[(String, String)]): Future[ClientKey] = {
    for {
      (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
      request = keyApi.getClientKeyById(xCorrelationId = correlationId, clientId, kid, xForwardedFor = ip)(
        BearerToken(bearerToken)
      )
      result <- invoker.invoke(request, "Key Retrieve")
    } yield result
  }

  override def deleteKey(clientId: UUID, kid: String)(implicit contexts: Seq[(String, String)]): Future[Unit] = {
    for {
      (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
      request = keyApi.deleteClientKeyById(xCorrelationId = correlationId, clientId, kid, xForwardedFor = ip)(
        BearerToken(bearerToken)
      )
      result <- invoker.invoke(request, "Key Delete")
    } yield result
  }

  override def getClientKeys(clientId: UUID)(implicit contexts: Seq[(String, String)]): Future[KeysResponse] = {
    for {
      (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
      request = keyApi.getClientKeys(xCorrelationId = correlationId, clientId, xForwardedFor = ip)(
        BearerToken(bearerToken)
      )
      result <- invoker.invoke(request, "Client keys retrieve")
    } yield result
  }

  def getEncodedClientKey(clientId: UUID, kid: String)(implicit
    contexts: Seq[(String, String)]
  ): Future[EncodedClientKey] = {
    for {
      (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
      request = keyApi.getEncodedClientKeyById(xCorrelationId = correlationId, clientId, kid, xForwardedFor = ip)(
        BearerToken(bearerToken)
      )
      result <- invoker.invoke(request, "Key Retrieve")
    } yield result
  }

  override def createKeys(clientId: UUID, keysSeeds: Seq[KeySeed])(implicit
    contexts: Seq[(String, String)]
  ): Future[KeysResponse] = {
    for {
      (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
      request = keyApi.createKeys(xCorrelationId = correlationId, clientId, keysSeeds, xForwardedFor = ip)(
        BearerToken(bearerToken)
      )
      result <- invoker.invoke(request, "Key creation")
    } yield result
  }

  override def addClientPurpose(clientId: UUID, purposeSeed: PurposeSeed)(implicit
    contexts: Seq[(String, String)]
  ): Future[Purpose] = {
    for {
      (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
      request = purposeApi.addClientPurpose(xCorrelationId = correlationId, clientId, purposeSeed, xForwardedFor = ip)(
        BearerToken(bearerToken)
      )
      result <- invoker.invoke(request, "Purpose addition to client")
    } yield result
  }

  override def removeClientPurpose(clientId: UUID, purposeId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[Unit] = {

    for {
      (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
      request = purposeApi.removeClientPurpose(xCorrelationId = correlationId, clientId, purposeId, xForwardedFor = ip)(
        BearerToken(bearerToken)
      )
      result <- invoker.invoke(request, "Purpose remove from client")
    } yield result

  }
}
