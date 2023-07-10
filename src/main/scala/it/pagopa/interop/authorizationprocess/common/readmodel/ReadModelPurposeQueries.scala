package it.pagopa.interop.authorizationprocess.common.readmodel

import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.purposemanagement.model.persistence.JsonFormats._
import it.pagopa.interop.purposemanagement.model.purpose._
import org.mongodb.scala.model.Filters

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

object ReadModelPurposeQueries extends ReadModelQuery {

  def getPurposeById(
    purposeId: UUID
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[Option[PersistentPurpose]] =
    readModel.findOne[PersistentPurpose]("purposes", Filters.eq("data.id", purposeId.toString))
}
