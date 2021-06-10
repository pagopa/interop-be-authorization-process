package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import java.util.UUID

trait UUIDSupplier {
  def get: UUID
}
