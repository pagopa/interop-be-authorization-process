package it.pagopa.interop.authorizationprocess.service

import scala.concurrent.Future
import it.pagopa.interop.commons.utils.extractHeaders
import it.pagopa.interop.commons.utils.AkkaUtils.getUid
import scala.util.{Failure, Success}

package object impl {
  def withHeaders[T](
    f: (String, String, Option[String]) => Future[T]
  )(implicit contexts: Seq[(String, String)]): Future[T] = extractHeaders(contexts) match {
    case Left(ex) => Future.failed(ex)
    case Right(x) => f.tupled(x)
  }

  def withUid[T](f: String => Future[T])(implicit contexts: Seq[(String, String)]): Future[T] = getUid(contexts) match {
    case Failure(ex) => Future.failed(ex)
    case Success(x)  => f(x)
  }
}
