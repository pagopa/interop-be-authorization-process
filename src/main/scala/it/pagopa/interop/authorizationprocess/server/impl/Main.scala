package it.pagopa.interop.authorizationprocess.server.impl

import cats.syntax.all._
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.scalalogging.Logger
import it.pagopa.interop.authorizationmanagement.model.client.{PersistentClientKind, PersistentClientStatesChain}
import it.pagopa.interop.authorizationmanagement.model.persistence.JsonFormats._
import it.pagopa.interop.authorizationmanagement.model.key.PersistentKey
import it.pagopa.interop.authorizationprocess.common.readmodel.model.ReadModelClientWithKeys
import it.pagopa.interop.authorizationprocess.common.readmodel.model.impl._
import it.pagopa.interop.authorizationprocess.service.{AuthorizationManagementService, PartyManagementService}
import it.pagopa.interop.commons.queue.message.Message.uuidFormat
import it.pagopa.interop.commons.utils.SprayCommonFormats.offsetDateTimeFormat
import org.mongodb.scala.model.Filters
import it.pagopa.interop.commons.utils.{BEARER, CORRELATION_ID_HEADER, UID}
import it.pagopa.interop.selfcare.partymanagement.client.invoker.ApiError
import it.pagopa.interop.selfcare.partymanagement.client.model.Relationship
import spray.json.DefaultJsonProtocol.jsonFormat9
import spray.json.RootJsonFormat

import java.time.OffsetDateTime
import scala.concurrent.duration.Duration
import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.{Await, ExecutionContext, Future}
import java.util.UUID
import scala.util.Failure

object Main extends App with Dependencies {

  val logger: Logger = Logger(this.getClass)

  implicit val context: List[(String, String)] = List(
    CORRELATION_ID_HEADER -> UUID.randomUUID().toString(),
    UID                   -> UUID.randomUUID().toString(),
    BEARER                -> "<bearer_token>"
  )

  implicit val actorSystem: ActorSystem[Nothing]  =
    ActorSystem[Nothing](Behaviors.empty, "interop-be-authorization-process-alignment")
  implicit val executionContext: ExecutionContext = actorSystem.executionContext

  implicit val es: ExecutorService = Executors.newFixedThreadPool(1.max(Runtime.getRuntime.availableProcessors() - 1))
  implicit val blockingEc          = ExecutionContext.fromExecutor(es)
  implicit val authorizationManagementService: AuthorizationManagementService = authorizationManagementService(
    blockingEc
  )
  implicit val partyManagementService: PartyManagementService                 = partyManagementService()

  final case class PersistentClientLocal(
    id: UUID,
    consumerId: UUID,
    name: String,
    purposes: Seq[PersistentClientStatesChain],
    description: Option[String],
    relationships: Set[UUID],
    users: Option[Set[UUID]],
    kind: PersistentClientKind,
    createdAt: OffsetDateTime
  )

  implicit val pcFormat: RootJsonFormat[PersistentClientLocal] = jsonFormat9(PersistentClientLocal.apply)

  logger.info("Starting update")
  logger.info(s"Retrieving clients")
  Await.result(
    execution()
      .andThen { case Failure(ex) => logger.error("Houston we have a problem", ex) }
      .andThen { _ =>
        es.shutdown()
      },
    Duration.Inf
  ): Unit

  logger.info("Completed update")

  def execution(): Future[Unit] = for {
    clients <- getClients()
    _ = logger.info(s"Start update clients ${clients.size}")
    _ <- clients.traverse(updateClient)
    _ = logger.info(s"End update clients")
    _ = logger.info(s"Retrieving keys")
    keys <- clients.traverse(getClientKeys).map(_.flatten)
    _ = logger.info(s"Start update keys ${keys.size}")
    _ <- keys
      .flatMap(client =>
        client.keys.collect { case PersistentKey(Some(relationshipId), None, kid, _, _, _, _, _) =>
          Parameter(client.id, kid, relationshipId)
        }
      )
      .traverse(keys => updateKeys(keys))
    _ = logger.info(s"End update keys")
  } yield ()

  def getRelationship(clientId: UUID, relationshipId: UUID): Future[Option[Relationship]] =
    partyManagementService
      .getRelationshipById(relationshipId)
      .map(Some(_))
      .recover {
        case e: ApiError[_] if e.code == 404 =>
          logger.warn(s"Relationship $relationshipId not found on selfcare (client $clientId")
          None
      }

  def updateClient(client: PersistentClientLocal): Future[Unit] = {
    logger.info(s"Update client ${client.id}")
    for {

      relationship <- client.relationships.toList.traverse(getRelationship(client.id, _))
      _            <- relationship.flatten.traverse(rel => {
        if (client.users.exists(users => users.contains(rel.from))) Future.unit
        else authorizationManagementService.addUser(client.id, rel.from)
      })
    } yield ()
  }

  def getClients(): Future[Seq[PersistentClientLocal]] =
    getAll(50)(readModelService.find[PersistentClientLocal]("clients", Filters.empty(), _, _))

  def getClientKeys(client: PersistentClientLocal): Future[Option[ReadModelClientWithKeys]] =
    readModelService.findOne[ReadModelClientWithKeys]("clients", Filters.eq("data.id", client.id.toString))

  def updateKeys(key: Parameter): Future[Unit] = {
    logger.info(s"Update keys for client ${key.clientId}")
    for {
      relationship <- getRelationship(key.clientId, key.relationShipId)
      _            <- relationship match {
        case Some(r) => authorizationManagementService.migrateKeyRelationshipToUser(key.clientId, key.kid, r.from)
        case None    =>
          logger.warn(
            s"Relationship ${key.relationShipId} not found for key ${key.kid} in client ${key.clientId}. Deleting key"
          )
          authorizationManagementService.deleteKey(key.clientId, key.kid)
      }
    } yield ()
  }

  def getAll[T](limit: Int)(get: (Int, Int) => Future[Seq[T]]): Future[Seq[T]] = {
    def go(offset: Int)(acc: Seq[T]): Future[Seq[T]] = {
      get(offset, limit).flatMap(xs =>
        if (xs.size < limit) Future.successful(xs ++ acc)
        else go(offset + xs.size)(xs ++ acc)
      )
    }
    go(0)(Nil)
  }

  final case class Parameter(clientId: UUID, kid: String, relationShipId: UUID)
}
