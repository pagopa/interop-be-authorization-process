package it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl

import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.UUIDSupplier

import java.util.UUID

class UUIDSupplierImpl extends UUIDSupplier {
  override def get: UUID = UUID.randomUUID()
}
