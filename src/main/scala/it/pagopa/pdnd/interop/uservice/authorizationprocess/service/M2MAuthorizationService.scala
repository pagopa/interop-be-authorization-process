package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import scala.concurrent.Future

trait M2MAuthorizationService {
  def token: Future[String]
}
