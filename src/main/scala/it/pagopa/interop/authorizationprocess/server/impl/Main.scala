package it.pagopa.interop.authorizationprocess.server.impl

import cats.syntax.all._
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.scalalogging.Logger

import it.pagopa.interop.authorizationmanagement.model.client.PersistentClient
import it.pagopa.interop.authorizationmanagement.model.persistence.JsonFormats._
import it.pagopa.interop.authorizationmanagement.model.key.PersistentKey
import it.pagopa.interop.authorizationprocess.common.readmodel.model.ReadModelClientWithKeys
import it.pagopa.interop.authorizationprocess.common.readmodel.model.impl._
import it.pagopa.interop.authorizationprocess.service.{AuthorizationManagementService, PartyManagementService}

import org.mongodb.scala.model.Filters
import it.pagopa.interop.commons.utils.CORRELATION_ID_HEADER

import scala.concurrent.duration.Duration
import java.util.concurrent.{Executors, ExecutorService}
import scala.concurrent.{ExecutionContext, Future, Await}

import java.util.UUID
import scala.util.Failure

object Main extends App with Dependencies {

  val logger: Logger = Logger(this.getClass)

  implicit val context: List[(String, String)] = (CORRELATION_ID_HEADER -> UUID.randomUUID().toString()) :: Nil

  implicit val actorSystem: ActorSystem[Nothing]  =
    ActorSystem[Nothing](Behaviors.empty, "interop-be-authorization-process-alignment")
  implicit val executionContext: ExecutionContext = actorSystem.executionContext

  implicit val es: ExecutorService = Executors.newFixedThreadPool(1.max(Runtime.getRuntime.availableProcessors() - 1))
  implicit val blockingEc          = ExecutionContext.fromExecutor(es)
  implicit val authorizationManagementService: AuthorizationManagementService = authorizationManagementService(
    blockingEc
  )
  implicit val partyManagementService: PartyManagementService                 = partyManagementService()

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
        client.keys.collect { case PersistentKey(Some(relationshipId), _, kid, _, _, _, _, _) =>
          Parameter(client.id, kid, relationshipId)
        }
      )
      .traverse(keys => updateKeys(keys))
    _ = logger.info(s"End update keys")
  } yield ()

  def updateClient(client: PersistentClient): Future[Unit] = {
    logger.info(s"Update client ${client.id}")
    for {

      relationship <- client.relationships.toList.traverse(partyManagementService.getRelationshipById)
      _            <- relationship.traverse(rel => authorizationManagementService.addUser(client.id, rel.from))
    } yield ()
  }

  def getClients(): Future[Seq[PersistentClient]] =
    getAll(50)(readModelService.find[PersistentClient]("clients", Filters.empty(), _, _))

  def getClientKeys(client: PersistentClient): Future[Option[ReadModelClientWithKeys]] =
    readModelService.findOne[ReadModelClientWithKeys]("clients", Filters.eq("data.id", client.id.toString))

  def updateKeys(key: Parameter): Future[Unit] = {
    logger.info(s"Update keys for client ${key.clientId}")
    for {
      relationship <- partyManagementService.getRelationshipById(key.relationShipId)
      _            <- authorizationManagementService.updateKey(key.clientId, key.kid, relationship.from)
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
