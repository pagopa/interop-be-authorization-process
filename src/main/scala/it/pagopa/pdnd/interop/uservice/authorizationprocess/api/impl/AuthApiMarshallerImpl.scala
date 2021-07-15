package it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.AuthApiMarshaller
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.{
  AccessTokenRequest,
  ClientCredentialsResponse,
  Problem
}

class AuthApiMarshallerImpl extends AuthApiMarshaller with SprayJsonSupport {

  override implicit def fromEntityUnmarshallerAccessTokenRequest: FromEntityUnmarshaller[AccessTokenRequest] =
    sprayJsonUnmarshaller[AccessTokenRequest]

  override implicit def toEntityMarshallerClientCredentialsResponse: ToEntityMarshaller[ClientCredentialsResponse] =
    sprayJsonMarshaller[ClientCredentialsResponse]

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]
}
