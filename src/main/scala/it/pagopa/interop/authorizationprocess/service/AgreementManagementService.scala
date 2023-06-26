package it.pagopa.interop.authorizationprocess.service

import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.agreementmanagement.model.agreement.PersistentAgreement
import it.pagopa.interop.authorizationprocess.model.{
  Agreement => ApiAgreement,
  EService => ApiEService,
  EServiceDescriptor => ApiEServiceDescriptor
}

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}

trait AgreementManagementService {
  def getAgreements(eserviceId: UUID, consumerId: UUID)(implicit
    ec: ExecutionContext,
    readModel: ReadModelService
  ): Future[Seq[PersistentAgreement]]
}

object AgreementManagementService {
  def agreementToApi(
    agreement: PersistentAgreement,
    eService: ApiEService,
    descriptor: ApiEServiceDescriptor
  ): ApiAgreement =
    ApiAgreement(id = agreement.id, eservice = eService, descriptor = descriptor)

}
