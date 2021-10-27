package it.pagopa.pdnd.interop.uservice.authorizationprocess.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive1, Route}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.http.scaladsl.unmarshalling.FromStringUnmarshaller
import it.pagopa.pdnd.interop.uservice.authorizationprocess.server.AkkaHttpHelper._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.Client
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.ClientKey
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.ClientKeys
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.ClientSeed
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.EncodedClientKey
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.KeySeed
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.Operator
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.OperatorSeed
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.Problem
import java.util.UUID

class ClientApi(
  clientService: ClientApiService,
  clientMarshaller: ClientApiMarshaller,
  wrappingDirective: Directive1[Seq[(String, String)]]
) {

  import clientMarshaller._

  lazy val route: Route =
    path("clients" / Segment / "activate") { (clientId) =>
      post {
        wrappingDirective { implicit contexts =>
          clientService.activateClientById(clientId = clientId)

        }
      }
    } ~
      path("clients" / Segment / "operators") { (clientId) =>
        post {
          wrappingDirective { implicit contexts =>
            entity(as[OperatorSeed]) { operatorSeed =>
              clientService.addOperator(clientId = clientId, operatorSeed = operatorSeed)
            }

          }
        }
      } ~
      path("clients") {
        post {
          wrappingDirective { implicit contexts =>
            entity(as[ClientSeed]) { clientSeed =>
              clientService.createClient(clientSeed = clientSeed)
            }

          }
        }
      } ~
      path("clients" / Segment / "keys") { (clientId) =>
        post {
          wrappingDirective { implicit contexts =>
            entity(as[Seq[KeySeed]]) { keySeed =>
              clientService.createKeys(clientId = clientId, keySeed = keySeed)
            }

          }
        }
      } ~
      path("clients" / Segment) { (clientId) =>
        delete {
          wrappingDirective { implicit contexts =>
            clientService.deleteClient(clientId = clientId)

          }
        }
      } ~
      path("clients" / Segment / "keys" / Segment) { (clientId, keyId) =>
        delete {
          wrappingDirective { implicit contexts =>
            clientService.deleteClientKeyById(clientId = clientId, keyId = keyId)

          }
        }
      } ~
      path("clients" / Segment / "keys" / Segment / "disable") { (clientId, keyId) =>
        patch {
          wrappingDirective { implicit contexts =>
            clientService.disableKeyById(clientId = clientId, keyId = keyId)

          }
        }
      } ~
      path("clients" / Segment / "keys" / Segment / "enable") { (clientId, keyId) =>
        patch {
          wrappingDirective { implicit contexts =>
            clientService.enableKeyById(clientId = clientId, keyId = keyId)

          }
        }
      } ~
      path("clients" / Segment) { (clientId) =>
        get {
          wrappingDirective { implicit contexts =>
            clientService.getClient(clientId = clientId)

          }
        }
      } ~
      path("clients" / Segment / "keys" / Segment) { (clientId, keyId) =>
        get {
          wrappingDirective { implicit contexts =>
            clientService.getClientKeyById(clientId = clientId, keyId = keyId)

          }
        }
      } ~
      path("clients" / Segment / "keys") { (clientId) =>
        get {
          wrappingDirective { implicit contexts =>
            clientService.getClientKeys(clientId = clientId)

          }
        }
      } ~
      path("clients" / Segment / "operators" / Segment) { (clientId, operatorId) =>
        get {
          wrappingDirective { implicit contexts =>
            clientService.getClientOperatorById(clientId = clientId, operatorId = operatorId)

          }
        }
      } ~
      path("clients" / Segment / "operators") { (clientId) =>
        get {
          wrappingDirective { implicit contexts =>
            clientService.getClientOperators(clientId = clientId)

          }
        }
      } ~
      path("clients" / Segment / "encoded" / "keys" / Segment) { (clientId, keyId) =>
        get {
          wrappingDirective { implicit contexts =>
            clientService.getEncodedClientKeyById(clientId = clientId, keyId = keyId)

          }
        }
      } ~
      path("clients") {
        get {
          wrappingDirective { implicit contexts =>
            parameters(
              "offset".as[Int].?,
              "limit".as[Int].?,
              "eServiceId".as[String].?,
              "operatorId".as[String].?,
              "consumerId".as[String].?
            ) { (offset, limit, eServiceId, operatorId, consumerId) =>
              clientService.listClients(
                offset = offset,
                limit = limit,
                eServiceId = eServiceId,
                operatorId = operatorId,
                consumerId = consumerId
              )

            }
          }
        }
      } ~
      path("clients" / Segment / "operators" / Segment) { (clientId, operatorId) =>
        delete {
          wrappingDirective { implicit contexts =>
            clientService.removeClientOperator(clientId = clientId, operatorId = operatorId)

          }
        }
      } ~
      path("clients" / Segment / "suspend") { (clientId) =>
        post {
          wrappingDirective { implicit contexts =>
            clientService.suspendClientById(clientId = clientId)

          }
        }
      }
}

trait ClientApiService {
  def activateClientById204: Route =
    complete((204, "the client has been activated."))
  def activateClientById400(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((400, responseProblem))
  def activateClientById401(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((401, responseProblem))
  def activateClientById404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 204, Message: the client has been activated.
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client not found, DataType: Problem
    */
  def activateClientById(
    clientId: String
  )(implicit contexts: Seq[(String, String)], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route

  def addOperator201(responseClient: Client)(implicit toEntityMarshallerClient: ToEntityMarshaller[Client]): Route =
    complete((201, responseClient))
  def addOperator400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    complete((400, responseProblem))
  def addOperator401(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    complete((401, responseProblem))
  def addOperator404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    complete((404, responseProblem))

  /** Code: 201, Message: Operator added, DataType: Client
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Missing Required Information, DataType: Problem
    */
  def addOperator(clientId: String, operatorSeed: OperatorSeed)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route

  def createClient201(responseClient: Client)(implicit toEntityMarshallerClient: ToEntityMarshaller[Client]): Route =
    complete((201, responseClient))
  def createClient401(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((401, responseProblem))
  def createClient404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 201, Message: Client created, DataType: Client
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Not Found, DataType: Problem
    */
  def createClient(clientSeed: ClientSeed)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route

  def createKeys201(responseClientKeys: ClientKeys)(implicit
    toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys]
  ): Route =
    complete((201, responseClientKeys))
  def createKeys400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    complete((400, responseProblem))
  def createKeys401(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    complete((401, responseProblem))
  def createKeys403(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    complete((403, responseProblem))
  def createKeys404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    complete((404, responseProblem))

  /** Code: 201, Message: Keys created, DataType: ClientKeys
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 403, Message: Forbidden, DataType: Problem
    * Code: 404, Message: Client id not found, DataType: Problem
    */
  def createKeys(clientId: String, keySeed: Seq[KeySeed])(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route

  def deleteClient204: Route =
    complete((204, "Client deleted"))
  def deleteClient401(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((401, responseProblem))
  def deleteClient404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 204, Message: Client deleted
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client not found, DataType: Problem
    */
  def deleteClient(
    clientId: String
  )(implicit contexts: Seq[(String, String)], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route

  def deleteClientKeyById204: Route =
    complete((204, "the corresponding key has been deleted."))
  def deleteClientKeyById401(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((401, responseProblem))
  def deleteClientKeyById404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 204, Message: the corresponding key has been deleted.
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Key not found, DataType: Problem
    */
  def deleteClientKeyById(clientId: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route

  def disableKeyById204: Route =
    complete((204, "the corresponding key has been disabled."))
  def disableKeyById401(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((401, responseProblem))
  def disableKeyById404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 204, Message: the corresponding key has been disabled.
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Key not found, DataType: Problem
    */
  def disableKeyById(clientId: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route

  def enableKeyById204: Route =
    complete((204, "the corresponding key has been enabled."))
  def enableKeyById401(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((401, responseProblem))
  def enableKeyById404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 204, Message: the corresponding key has been enabled.
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Key not found, DataType: Problem
    */
  def enableKeyById(clientId: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route

  def getClient200(responseClient: Client)(implicit toEntityMarshallerClient: ToEntityMarshaller[Client]): Route =
    complete((200, responseClient))
  def getClient401(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    complete((401, responseProblem))
  def getClient404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    complete((404, responseProblem))

  /** Code: 200, Message: Client retrieved, DataType: Client
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client not found, DataType: Problem
    */
  def getClient(clientId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route

  def getClientKeyById200(responseClientKey: ClientKey)(implicit
    toEntityMarshallerClientKey: ToEntityMarshaller[ClientKey]
  ): Route =
    complete((200, responseClientKey))
  def getClientKeyById401(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((401, responseProblem))
  def getClientKeyById404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 200, Message: returns the corresponding key, DataType: ClientKey
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Key not found, DataType: Problem
    */
  def getClientKeyById(clientId: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientKey: ToEntityMarshaller[ClientKey],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route

  def getClientKeys200(responseClientKeys: ClientKeys)(implicit
    toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys]
  ): Route =
    complete((200, responseClientKeys))
  def getClientKeys401(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((401, responseProblem))
  def getClientKeys404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 200, Message: returns the corresponding array of keys, DataType: ClientKeys
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client id not found, DataType: Problem
    */
  def getClientKeys(clientId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route

  def getClientOperatorById200(responseOperator: Operator)(implicit
    toEntityMarshallerOperator: ToEntityMarshaller[Operator]
  ): Route =
    complete((200, responseOperator))
  def getClientOperatorById401(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((401, responseProblem))
  def getClientOperatorById404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 200, Message: Client Operator retrieved, DataType: Operator
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client or Operator not found, DataType: Problem
    */
  def getClientOperatorById(clientId: String, operatorId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerOperator: ToEntityMarshaller[Operator],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route

  def getClientOperators200(responseOperatorarray: Seq[Operator])(implicit
    toEntityMarshallerOperatorarray: ToEntityMarshaller[Seq[Operator]]
  ): Route =
    complete((200, responseOperatorarray))
  def getClientOperators401(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((401, responseProblem))
  def getClientOperators404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 200, Message: Request succeed, DataType: Seq[Operator]
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Not Found, DataType: Problem
    */
  def getClientOperators(clientId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerOperatorarray: ToEntityMarshaller[Seq[Operator]],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route

  def getEncodedClientKeyById200(responseEncodedClientKey: EncodedClientKey)(implicit
    toEntityMarshallerEncodedClientKey: ToEntityMarshaller[EncodedClientKey]
  ): Route =
    complete((200, responseEncodedClientKey))
  def getEncodedClientKeyById401(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((401, responseProblem))
  def getEncodedClientKeyById404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 200, Message: returns the corresponding base 64 encoded key, DataType: EncodedClientKey
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Key not found, DataType: Problem
    */
  def getEncodedClientKeyById(clientId: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerEncodedClientKey: ToEntityMarshaller[EncodedClientKey],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route

  def listClients200(responseClientarray: Seq[Client])(implicit
    toEntityMarshallerClientarray: ToEntityMarshaller[Seq[Client]]
  ): Route =
    complete((200, responseClientarray))
  def listClients400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    complete((400, responseProblem))
  def listClients401(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    complete((401, responseProblem))

  /** Code: 200, Message: Request succeed, DataType: Seq[Client]
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 401, Message: Unauthorized, DataType: Problem
    */
  def listClients(
    offset: Option[Int],
    limit: Option[Int],
    eServiceId: Option[String],
    operatorId: Option[String],
    consumerId: Option[String]
  )(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClientarray: ToEntityMarshaller[Seq[Client]]
  ): Route

  def removeClientOperator204: Route =
    complete((204, "Operator removed"))
  def removeClientOperator400(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((400, responseProblem))
  def removeClientOperator401(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((401, responseProblem))
  def removeClientOperator404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 204, Message: Operator removed
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client or operator not found, DataType: Problem
    */
  def removeClientOperator(clientId: String, operatorId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route

  def suspendClientById204: Route =
    complete((204, "the corresponding client has been suspended."))
  def suspendClientById400(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((400, responseProblem))
  def suspendClientById401(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((401, responseProblem))
  def suspendClientById404(responseProblem: Problem)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route =
    complete((404, responseProblem))

  /** Code: 204, Message: the corresponding client has been suspended.
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client not found, DataType: Problem
    */
  def suspendClientById(
    clientId: String
  )(implicit contexts: Seq[(String, String)], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route

}

trait ClientApiMarshaller {
  implicit def fromEntityUnmarshallerKeySeedList: FromEntityUnmarshaller[Seq[KeySeed]]

  implicit def fromEntityUnmarshallerClientSeed: FromEntityUnmarshaller[ClientSeed]

  implicit def fromEntityUnmarshallerOperatorSeed: FromEntityUnmarshaller[OperatorSeed]

  implicit def toEntityMarshallerOperator: ToEntityMarshaller[Operator]

  implicit def toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys]

  implicit def toEntityMarshallerClientKey: ToEntityMarshaller[ClientKey]

  implicit def toEntityMarshallerOperatorarray: ToEntityMarshaller[Seq[Operator]]

  implicit def toEntityMarshallerEncodedClientKey: ToEntityMarshaller[EncodedClientKey]

  implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem]

  implicit def toEntityMarshallerClientarray: ToEntityMarshaller[Seq[Client]]

  implicit def toEntityMarshallerClient: ToEntityMarshaller[Client]

}
