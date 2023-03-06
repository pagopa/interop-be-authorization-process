package it.pagopa.interop.authorizationprocess.common

import it.pagopa.interop.authorizationprocess.model._
import it.pagopa.interop.authorizationmanagement.model.client.PersistentClient

object Adapters {

  implicit class PersistentClientWrapper(private val p: PersistentClient) extends AnyVal {
    def toApi: Client = Client(
      id = p.id,
      consumer = Organization("id", "name"),
      name = p.name,
      purposes = Seq.empty,
      description = None,
      operators = None,
      kind = ClientKind.CONSUMER
    )
  }

}
