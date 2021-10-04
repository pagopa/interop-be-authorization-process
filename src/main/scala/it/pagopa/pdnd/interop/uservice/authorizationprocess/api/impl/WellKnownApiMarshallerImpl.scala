package it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.WellKnownApiMarshaller
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._

case object WellKnownApiMarshallerImpl extends WellKnownApiMarshaller with SprayJsonSupport {

  override implicit def toEntityMarshallerKeysResponse: ToEntityMarshaller[KeysResponse] =
    sprayJsonMarshaller[KeysResponse]

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]
}
