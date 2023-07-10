package it.pagopa.interop.authorizationprocess.service.impl

import it.pagopa.interop.authorizationprocess.service.CatalogManagementService

import it.pagopa.interop.catalogmanagement.model.CatalogItem
import it.pagopa.interop.authorizationprocess.common.readmodel.ReadModelCatalogQueries
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.authorizationprocess.error.AuthorizationProcessErrors.EServiceNotFound

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}

object CatalogManagementServiceImpl extends CatalogManagementService {

  override def getEServiceById(
    eServiceId: UUID
  )(implicit ec: ExecutionContext, readModel: ReadModelService): Future[CatalogItem] = {
    ReadModelCatalogQueries.getEServiceById(eServiceId).flatMap(_.toFuture(EServiceNotFound(eServiceId)))
  }
}
