package it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.AuthApiMarshaller
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._

case object AuthApiMarshallerImpl extends AuthApiMarshaller with SprayJsonSupport {

  override implicit def toEntityMarshallerClientCredentialsResponse: ToEntityMarshaller[ClientCredentialsResponse] =
    sprayJsonMarshaller[ClientCredentialsResponse]

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]

  override implicit def toEntityMarshallerValidJWT: ToEntityMarshaller[ValidJWT] = sprayJsonMarshaller[ValidJWT]
}
