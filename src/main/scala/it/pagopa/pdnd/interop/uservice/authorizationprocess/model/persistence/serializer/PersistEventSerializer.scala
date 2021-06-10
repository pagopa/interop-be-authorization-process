package it.pagopa.pdnd.interop.uservice.authorizationprocess.model.persistence.serializer

import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.utils.ErrorOr

trait PersistEventSerializer[A, B] {
  def to(a: A): ErrorOr[B]
}

object PersistEventSerializer {
  def to[A, B](a: A)(implicit e: PersistEventSerializer[A, B]): ErrorOr[B] = e.to(a)
}
