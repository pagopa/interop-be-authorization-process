package it.pagopa.pdnd.interop.uservice.authorizationprocess.model.persistence.serializer

import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.utils.ErrorOr

trait PersistEventDeserializer[A, B] {
  def from(a: A): ErrorOr[B]
}

object PersistEventDeserializer {
  def from[A, B](a: A)(implicit e: PersistEventDeserializer[A, B]): ErrorOr[B] = e.from(a)
}
