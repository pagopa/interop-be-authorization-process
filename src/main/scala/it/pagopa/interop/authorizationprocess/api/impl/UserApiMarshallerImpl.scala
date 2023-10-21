package it.pagopa.interop.authorizationprocess.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.interop.authorizationprocess.api.UserApiMarshaller
import it.pagopa.interop.authorizationprocess.model._

case object UserApiMarshallerImpl extends UserApiMarshaller with SprayJsonSupport {

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = entityMarshallerProblem

  override implicit def toEntityMarshallerKeys: ToEntityMarshaller[Keys] = sprayJsonMarshaller[Keys]
}
