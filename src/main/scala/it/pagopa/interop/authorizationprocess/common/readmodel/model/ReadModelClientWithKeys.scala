package it.pagopa.interop.authorizationprocess.common.readmodel.model

import it.pagopa.interop.authorizationmanagement.model.client._
import it.pagopa.interop.authorizationmanagement.model.key.PersistentKey
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

import it.pagopa.interop.authorizationmanagement.model.persistence.JsonFormats._
import it.pagopa.interop.commons.utils.SprayCommonFormats.uuidFormat
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import java.util.UUID

final case class ReadModelClientWithKeys(
  id: UUID,
  consumerId: UUID,
  name: String,
  purposes: Seq[PersistentClientStatesChain],
  description: Option[String],
  relationships: Set[UUID],
  kind: PersistentClientKind,
  keys: Seq[PersistentKey]
)

object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val rmcwkFormat: RootJsonFormat[ReadModelClientWithKeys] =
    jsonFormat8(ReadModelClientWithKeys)

}
