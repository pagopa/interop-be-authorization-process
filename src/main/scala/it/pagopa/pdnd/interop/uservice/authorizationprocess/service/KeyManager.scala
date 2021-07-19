package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import it.pagopa.pdnd.interop.uservice.keymanagement.client.model.Key

import scala.concurrent.Future

trait KeyManager {
  def getKey(clientId: String, kid: String): Future[Key]
}
