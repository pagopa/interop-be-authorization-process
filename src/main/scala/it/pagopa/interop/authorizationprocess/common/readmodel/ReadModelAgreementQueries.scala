package it.pagopa.interop.authorizationprocess.common.readmodel

import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.agreementmanagement.model.persistence.JsonFormats._
import it.pagopa.interop.agreementmanagement.model.agreement.PersistentAgreement
import org.mongodb.scala.model.Filters

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

object ReadModelAgreementQueries extends ReadModelQuery {

  def getAgreements(eServiceId: UUID, consumerId: UUID, offset: Int, limit: Int)(implicit
    ec: ExecutionContext,
    readModel: ReadModelService
  ): Future[Seq[PersistentAgreement]] = {
    val eserviceIdFilter = Filters.eq("data.eserviceId", eServiceId.toString)
    val consumerIdFilter = Filters.eq("data.consumerId", consumerId.toString)

    readModel.find[PersistentAgreement]("agreements", Filters.and(eserviceIdFilter, consumerIdFilter), offset, limit)
  }
}
