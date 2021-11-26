package it.pagopa.pdnd.interop.uservice.authorizationprocess.common

package object utils {
  type ErrorOr[A] = Either[Throwable, A]
  final val expireIn: Long = 600000L
}
