package it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.ClientApiMarshaller
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._

case object ClientApiMarshallerImpl extends ClientApiMarshaller with SprayJsonSupport {

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]

  override implicit def fromEntityUnmarshallerClientSeed: FromEntityUnmarshaller[ClientSeed] =
    sprayJsonUnmarshaller[ClientSeed]

  override implicit def fromEntityUnmarshallerOperatorSeed: FromEntityUnmarshaller[OperatorSeed] =
    sprayJsonUnmarshaller[OperatorSeed]

  override implicit def toEntityMarshallerClientKey: ToEntityMarshaller[ClientKey] = sprayJsonMarshaller[ClientKey]

  override implicit def fromEntityUnmarshallerKeySeedList: FromEntityUnmarshaller[Seq[KeySeed]] =
    sprayJsonUnmarshaller[Seq[KeySeed]]

  override implicit def toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys] = sprayJsonMarshaller[ClientKeys]

  override implicit def toEntityMarshallerClient: ToEntityMarshaller[Client] = sprayJsonMarshaller[Client]

  override implicit def toEntityMarshallerClientarray: ToEntityMarshaller[Seq[Client]] =
    sprayJsonMarshaller[Seq[Client]]

  override implicit def toEntityMarshallerOperatorarray: ToEntityMarshaller[Seq[Operator]] =
    sprayJsonMarshaller[Seq[Operator]]

  override implicit def toEntityMarshallerOperator: ToEntityMarshaller[Operator] = sprayJsonMarshaller[Operator]
}
