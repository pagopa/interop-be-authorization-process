package it.pagopa.interop.authorizationprocess.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import it.pagopa.interop.authorizationprocess.api.OperatorApiMarshaller
import it.pagopa.interop.authorizationprocess.model._

case object OperatorApiMarshallerImpl extends OperatorApiMarshaller with SprayJsonSupport {

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]

  override implicit def toEntityMarshallerClientKey: ToEntityMarshaller[ClientKey] = sprayJsonMarshaller[ClientKey]

  override implicit def toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys] = sprayJsonMarshaller[ClientKeys]

  override implicit def fromEntityUnmarshallerOperatorKeySeedList: FromEntityUnmarshaller[Seq[OperatorKeySeed]] =
    sprayJsonUnmarshaller[Seq[OperatorKeySeed]]
}
