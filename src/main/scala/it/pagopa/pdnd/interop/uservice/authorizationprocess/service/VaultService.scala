package it.pagopa.pdnd.interop.uservice.authorizationprocess.service

import scala.util.Try

trait VaultService {
  def getSecret(path: String): Map[String, String]
}

object VaultService {

  private val keyRootPath: Try[String] = Try(System.getenv("PDND_INTEROP_PRIVATE_KEY"))

  def extractKeyPath(algorithm: String, kind: String): Try[String] =
    keyRootPath.map(root => s"$root/$algorithm/jwk/$kind")

}
