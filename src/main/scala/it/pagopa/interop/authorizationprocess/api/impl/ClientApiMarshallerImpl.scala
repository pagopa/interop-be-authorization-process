package it.pagopa.interop.authorizationprocess.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import it.pagopa.interop.authorizationprocess.api.ClientApiMarshaller
import it.pagopa.interop.authorizationprocess.model._

case object ClientApiMarshallerImpl extends ClientApiMarshaller with SprayJsonSupport {

  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = entityMarshallerProblem

  override implicit def fromEntityUnmarshallerClientSeed: FromEntityUnmarshaller[ClientSeed] =
    sprayJsonUnmarshaller[ClientSeed]

  override implicit def toEntityMarshallerEncodedClientKey: ToEntityMarshaller[EncodedClientKey] =
    sprayJsonMarshaller[EncodedClientKey]

  override implicit def fromEntityUnmarshallerKeySeedList: FromEntityUnmarshaller[Seq[KeySeed]] =
    sprayJsonUnmarshaller[Seq[KeySeed]]

  override implicit def toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys] = sprayJsonMarshaller[ClientKeys]

  override implicit def toEntityMarshallerClient: ToEntityMarshaller[Client] = sprayJsonMarshaller[Client]

  implicit def toEntityMarshallerClientarray: ToEntityMarshaller[Seq[Client]] =
    sprayJsonMarshaller[Seq[Client]]

  override implicit def toEntityMarshallerClients: ToEntityMarshaller[Clients] =
    sprayJsonMarshaller[Clients]

  override implicit def toEntityMarshallerOperatorarray: ToEntityMarshaller[Seq[Operator]] =
    sprayJsonMarshaller[Seq[Operator]]

  override implicit def toEntityMarshallerOperator: ToEntityMarshaller[Operator] = sprayJsonMarshaller[Operator]

  override implicit def fromEntityUnmarshallerPurposeAdditionDetails: FromEntityUnmarshaller[PurposeAdditionDetails] =
    sprayJsonUnmarshaller[PurposeAdditionDetails]

  override implicit def toEntityMarshallerReadClientKey: ToEntityMarshaller[ReadClientKey] =
    sprayJsonMarshaller[ReadClientKey]

  override implicit def toEntityMarshallerReadClientKeys: ToEntityMarshaller[ReadClientKeys] =
    sprayJsonMarshaller[ReadClientKeys]

  override implicit def toEntityMarshallerClientEntry: ToEntityMarshaller[ClientEntry] =
    sprayJsonMarshaller[ClientEntry]

}
