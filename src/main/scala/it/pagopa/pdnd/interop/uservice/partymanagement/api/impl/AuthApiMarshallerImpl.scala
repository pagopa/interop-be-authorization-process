package it.pagopa.pdnd.interop.uservice.partymanagement.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import it.pagopa.pdnd.interop.uservice.securityprocess.api.AuthApiMarshaller
import it.pagopa.pdnd.interop.uservice.securityprocess.model.{
  ClientCredentialsRequest,
  ClientCredentialsResponse,
  Problem
}

class AuthApiMarshallerImpl extends AuthApiMarshaller with SprayJsonSupport {
  override implicit def fromEntityUnmarshallerClientCredentialsRequest
    : FromEntityUnmarshaller[ClientCredentialsRequest] = sprayJsonUnmarshaller[ClientCredentialsRequest]

  override implicit def toEntityMarshallerClientCredentialsResponse: ToEntityMarshaller[ClientCredentialsResponse] =
    sprayJsonMarshaller[ClientCredentialsResponse]

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]
}
