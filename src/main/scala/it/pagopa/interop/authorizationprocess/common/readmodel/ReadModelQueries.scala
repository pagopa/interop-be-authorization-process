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

object ReadModelQueries {

  def listClients(name: Option[String],
    relationshipIds: List[UUID],
    consumerId: UUID,
    purposeId: Option[UUID],
    kind: Option[PersistentClientKind],
    offset: Int,
    limit: Int)(readModel: ReadModelService)(implicit ec: ExecutionContext): Future[PaginatedResult[PersistentClient]] = 
      listGenericClients[PersistentClient](
        name = name,
        relationshipIds = relationshipIds,
        consumerId = consumerId,
        purposeId = purposeId,
        kind = kind,
        offset = offset,
        limit = limit
      )(readModel)


  def listClientsWithKeys(
    name: Option[String],
    relationshipIds: List[UUID],
    consumerId: UUID,
    purposeId: Option[UUID],
    kind: Option[PersistentClientKind],
    offset: Int,
    limit: Int
  )(readModel: ReadModelService)(implicit ec: ExecutionContext): Future[PaginatedResult[ReadModelClientWithKeys]] = listGenericClients[ReadModelClientWithKeys](
    name = name,
    relationshipIds = relationshipIds,
    consumerId = consumerId,
    purposeId = purposeId,
    kind = kind,
    offset = offset,
    limit = limit
  )(readModel)

  private def listGenericClients[A: JsonReader](
    name: Option[String],
    relationshipIds: List[UUID],
    consumerId: UUID,
    purposeId: Option[UUID],
    kind: Option[PersistentClientKind],
    offset: Int,
    limit: Int
  )(readModel: ReadModelService)(implicit ec: ExecutionContext): Future[PaginatedResult[A]] = {

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

  def mapToVarArgs[A, B](l: Seq[A])(f: Seq[A] => B): Option[B] = Option.when(l.nonEmpty)(f(l))

}
