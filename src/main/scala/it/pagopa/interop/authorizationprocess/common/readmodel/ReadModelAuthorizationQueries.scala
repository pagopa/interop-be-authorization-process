package it.pagopa.interop.authorizationprocess.common.readmodel

import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.authorizationmanagement.model.client.{PersistentClientKind, PersistentClient}
import it.pagopa.interop.authorizationmanagement.model.persistence.JsonFormats._
import it.pagopa.interop.authorizationprocess.common.readmodel.model.ReadModelClientWithKeys
import it.pagopa.interop.authorizationprocess.common.readmodel.model.impl._
import org.mongodb.scala.Document
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Aggregates.{`match`, count, project, sort}
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Projections.{computed, fields, include}
import org.mongodb.scala.model.Sorts.ascending

import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID
import spray.json._

object ReadModelAuthorizationQueries extends ReadModelQuery {

  def getClientById(
    clientId: UUID
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[Option[PersistentClient]] = readModel
    .findOne[PersistentClient]("clients", Filters.eq("data.id", clientId.toString))

  def getClientKey(clientId: UUID, kid: Option[String])(implicit
    ec: ExecutionContext,
    readModel: ReadModelService
  ): Future[Option[ReadModelClientWithKeys]] = {

    val clientFilter = Filters.eq("data.id", clientId.toString)
    val kidFilter    = kid.map(Filters.eq("data.keys.kid", _))

    val filter = mapToVarArgs(kidFilter.toList :+ clientFilter)(Filters.and).getOrElse(Filters.empty())

    readModel
      .findOne[ReadModelClientWithKeys]("clients", filter)
  }

  def getClientKeys(
    clientId: UUID
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[Option[ReadModelClientWithKeys]] =
    readModel
      .findOne[ReadModelClientWithKeys]("clients", Filters.eq("data.id", clientId.toString))

  def getClients(
    name: Option[String],
    relationshipIds: List[UUID],
    consumerId: UUID,
    purposeId: Option[UUID],
    kind: Option[PersistentClientKind],
    offset: Int,
    limit: Int
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PaginatedResult[PersistentClient]] =
    listGenericClients[PersistentClient](
      name = name,
      relationshipIds = relationshipIds,
      consumerId = consumerId,
      purposeId = purposeId,
      kind = kind,
      offset = offset,
      limit = limit
    )

  def getClientsWithKeys(
    name: Option[String],
    relationshipIds: List[UUID],
    consumerId: UUID,
    purposeId: Option[UUID],
    kind: Option[PersistentClientKind],
    offset: Int,
    limit: Int
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PaginatedResult[ReadModelClientWithKeys]] =
    listGenericClients[ReadModelClientWithKeys](
      name = name,
      relationshipIds = relationshipIds,
      consumerId = consumerId,
      purposeId = purposeId,
      kind = kind,
      offset = offset,
      limit = limit
    )

  private def listGenericClients[A: JsonReader](
    name: Option[String],
    relationshipIds: List[UUID],
    consumerId: UUID,
    purposeId: Option[UUID],
    kind: Option[PersistentClientKind],
    offset: Int,
    limit: Int
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[PaginatedResult[A]] = {

    val query: Bson =
      listClientsFilters(name, relationshipIds.map(_.toString), consumerId.toString, purposeId.map(_.toString), kind)

    val filterPipeline: Seq[Bson] = Seq(`match`(query))

    val countPipeline: Seq[Bson] = {
      Seq(count("totalCount"), project(computed("data", Document("""{ "totalCount" : "$totalCount" }"""))))
    }

    for {
      clients <- readModel.aggregate[A](
        "clients",
        filterPipeline ++
          Seq(
            project(fields(include("data"), computed("lowerName", Document("""{ "$toLower" : "$data.name" }""")))),
            sort(ascending("lowerName"))
          ),
        offset = offset,
        limit = limit
      )
      count   <- readModel
        .aggregate[TotalCountResult]("clients", filterPipeline ++ countPipeline, offset = 0, limit = Int.MaxValue)
    } yield PaginatedResult(results = clients, totalCount = count.headOption.map(_.totalCount).getOrElse(0))
  }

  private def listClientsFilters(
    name: Option[String],
    relationshipIds: List[String],
    consumerId: String,
    purposeId: Option[String],
    kind: Option[PersistentClientKind]
  ): Bson = {
    val relationshipIdsFilter = mapToVarArgs(relationshipIds.map(Filters.eq("data.relationships", _)))(Filters.or)
    val nameFilter            = name.map(Filters.regex("data.name", _, "i"))
    val kindFilter            = kind.map(k => Filters.eq("data.kind", k.toString))

    val consumerFilter = Filters.eq("data.consumerId", consumerId)
    val purposeFilter  = purposeId.map(Filters.eq("data.purposes.purpose.purposeId", _))

    mapToVarArgs(
      relationshipIdsFilter.toList ++ nameFilter.toList ++ kindFilter.toList ++ purposeFilter.toList :+ consumerFilter
    )(Filters.and).getOrElse(Filters.empty())
  }

  def getClientsByPurpose(
    purposeId: UUID
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[Seq[PersistentClient]] = {

    val query: Bson = Filters
      .and(Filters.eq("data.purposes.purpose.purposeId", purposeId.toString))

    readModel.find[PersistentClient]("clients", query, offset = 0, limit = Int.MaxValue)
  }
}
