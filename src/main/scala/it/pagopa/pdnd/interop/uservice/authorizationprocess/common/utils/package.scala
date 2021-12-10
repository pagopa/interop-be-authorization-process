package it.pagopa.pdnd.interop.uservice.authorizationprocess.common

import it.pagopa.pdnd.interop.commons.jwt.service.JWTReader
import it.pagopa.pdnd.interop.commons.utils.TypeConversions.TryOps
import it.pagopa.pdnd.interop.commons.utils.AkkaUtils.getFutureBearer

import scala.concurrent.{ExecutionContext, Future}

package object utils {
  type ErrorOr[A] = Either[Throwable, A]
  final val expireIn: Long = 600000L

  def validateClientBearer(contexts: Seq[(String, String)], jwt: JWTReader)(implicit
    ec: ExecutionContext
  ): Future[String] =
    for {
      bearer <- getFutureBearer(contexts)
      _      <- jwt.getClaims(bearer).toFuture
    } yield bearer

}
