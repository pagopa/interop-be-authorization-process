package it.pagopa.interop.authorizationprocess.service.impl

import it.pagopa.interop.authorizationprocess.service.AgreementManagementService
import it.pagopa.interop.agreementmanagement.model.agreement.PersistentAgreement
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.authorizationprocess.common.readmodel.ReadModelAgreementQueries

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}

final object AgreementManagementServiceImpl extends AgreementManagementService {

  override def getAgreements(eserviceId: UUID, consumerId: UUID)(implicit
    ec: ExecutionContext,
    readModel: ReadModelService
  ): Future[Seq[PersistentAgreement]] =
    getAllAgreements(eserviceId, consumerId)

  private def getAgreements(eserviceId: UUID, consumerId: UUID, offset: Int, limit: Int)(implicit
    ec: ExecutionContext,
    readModel: ReadModelService
  ): Future[Seq[PersistentAgreement]] =
    ReadModelAgreementQueries.getAgreements(eserviceId, consumerId, offset, limit)

  private def getAllAgreements(eserviceId: UUID, consumerId: UUID)(implicit
    ec: ExecutionContext,
    readModel: ReadModelService
  ): Future[Seq[PersistentAgreement]] = {

    def getAgreementsFrom(offset: Int): Future[Seq[PersistentAgreement]] =
      getAgreements(eserviceId = eserviceId, consumerId = consumerId, offset = offset, limit = 50)

    def go(start: Int)(as: Seq[PersistentAgreement]): Future[Seq[PersistentAgreement]] =
      getAgreementsFrom(start).flatMap(esec =>
        if (esec.size < 50) Future.successful(as ++ esec) else go(start + 50)(as ++ esec)
      )

    go(0)(Nil)
  }
}
