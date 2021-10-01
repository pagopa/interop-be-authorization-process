package it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.OperatorApiMarshaller
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._

final case class OperatorApiMarshallerImpl() extends OperatorApiMarshaller with SprayJsonSupport {

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]

  override implicit def toEntityMarshallerClientKey: ToEntityMarshaller[ClientKey] = sprayJsonMarshaller[ClientKey]

  override implicit def toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys] = sprayJsonMarshaller[ClientKeys]

  override implicit def fromEntityUnmarshallerOperatorKeySeedList: FromEntityUnmarshaller[Seq[OperatorKeySeed]] =
    sprayJsonUnmarshaller[Seq[OperatorKeySeed]]
}
