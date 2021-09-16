package it.pagopa.pdnd.interop.uservice.authorizationprocess.service.impl

import it.pagopa.pdnd.interop.uservice.authorizationprocess.service.M2MAuthorizationService

import scala.concurrent.Future

final case class M2MAuthorizationServiceImpl() extends M2MAuthorizationService {
  override def token: Future[String] = Future.successful("m2mtoken") // TODO Implement me
}
