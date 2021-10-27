package it.pagopa.pdnd.interop.uservice.authorizationprocess.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive1, Route}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
    import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
    import akka.http.scaladsl.unmarshalling.FromStringUnmarshaller
import it.pagopa.pdnd.interop.uservice.authorizationprocess.server.AkkaHttpHelper._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.ClientKey
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.ClientKeys
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.OperatorKeySeed
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.Problem
import java.util.UUID


    class OperatorApi(
    operatorService: OperatorApiService,
    operatorMarshaller: OperatorApiMarshaller,
    wrappingDirective: Directive1[Seq[(String, String)]]
    ) {
    
    import operatorMarshaller._

    lazy val route: Route =
        path("operators" / Segment / "keys") { (operatorId) => 
        post { wrappingDirective { implicit contexts =>  
entity(as[Seq[OperatorKeySeed]]){ operatorKeySeed =>
operatorService.createOperatorKeys(operatorId = operatorId, operatorKeySeed = operatorKeySeed)
    }



        }
        }
        } ~
        path("operators" / Segment / "keys" / Segment) { (operatorId, keyId) => 
        delete { wrappingDirective { implicit contexts =>  
operatorService.deleteOperatorKeyById(operatorId = operatorId, keyId = keyId)



        }
        }
        } ~
        path("operators" / Segment / "keys" / Segment / "disable") { (operatorId, keyId) => 
        patch { wrappingDirective { implicit contexts =>  
operatorService.disableOperatorKeyById(operatorId = operatorId, keyId = keyId)



        }
        }
        } ~
        path("operators" / Segment / "keys" / Segment / "enable") { (operatorId, keyId) => 
        patch { wrappingDirective { implicit contexts =>  
operatorService.enableOperatorKeyById(operatorId = operatorId, keyId = keyId)



        }
        }
        } ~
        path("clients" / Segment / "operators" / Segment / "keys") { (clientId, operatorId) => 
        get { wrappingDirective { implicit contexts =>  
operatorService.getClientOperatorKeys(clientId = clientId, operatorId = operatorId)



        }
        }
        } ~
        path("operators" / Segment / "keys" / Segment) { (operatorId, keyId) => 
        get { wrappingDirective { implicit contexts =>  
operatorService.getOperatorKeyById(operatorId = operatorId, keyId = keyId)



        }
        }
        } ~
        path("operators" / Segment / "keys") { (operatorId) => 
        get { wrappingDirective { implicit contexts =>  
operatorService.getOperatorKeys(operatorId = operatorId)



        }
        }
        }
    }


    trait OperatorApiService {
          def createOperatorKeys201(responseClientKeys: ClientKeys)(implicit toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys]): Route =
            complete((201, responseClientKeys))
  def createOperatorKeys400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((400, responseProblem))
  def createOperatorKeys401(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((401, responseProblem))
  def createOperatorKeys403(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((403, responseProblem))
  def createOperatorKeys404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((404, responseProblem))
        /**
           * Code: 201, Message: Keys created, DataType: ClientKeys
   * Code: 400, Message: Bad Request, DataType: Problem
   * Code: 401, Message: Unauthorized, DataType: Problem
   * Code: 403, Message: Forbidden, DataType: Problem
   * Code: 404, Message: Client id not found, DataType: Problem
        */
        def createOperatorKeys(operatorId: String, operatorKeySeed: Seq[OperatorKeySeed])
        (implicit contexts: Seq[(String, String)], toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route

          def deleteOperatorKeyById204: Route =
            complete((204, "the corresponding key has been deleted."))
  def deleteOperatorKeyById401(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((401, responseProblem))
  def deleteOperatorKeyById404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((404, responseProblem))
        /**
           * Code: 204, Message: the corresponding key has been deleted.
   * Code: 401, Message: Unauthorized, DataType: Problem
   * Code: 404, Message: Key not found, DataType: Problem
        */
        def deleteOperatorKeyById(operatorId: String, keyId: String)
        (implicit contexts: Seq[(String, String)], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route

          def disableOperatorKeyById204: Route =
            complete((204, "the corresponding key has been disabled."))
  def disableOperatorKeyById401(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((401, responseProblem))
  def disableOperatorKeyById404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((404, responseProblem))
        /**
           * Code: 204, Message: the corresponding key has been disabled.
   * Code: 401, Message: Unauthorized, DataType: Problem
   * Code: 404, Message: Key not found, DataType: Problem
        */
        def disableOperatorKeyById(operatorId: String, keyId: String)
        (implicit contexts: Seq[(String, String)], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route

          def enableOperatorKeyById204: Route =
            complete((204, "the corresponding key has been enabled."))
  def enableOperatorKeyById401(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((401, responseProblem))
  def enableOperatorKeyById404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((404, responseProblem))
        /**
           * Code: 204, Message: the corresponding key has been enabled.
   * Code: 401, Message: Unauthorized, DataType: Problem
   * Code: 404, Message: Key not found, DataType: Problem
        */
        def enableOperatorKeyById(operatorId: String, keyId: String)
        (implicit contexts: Seq[(String, String)], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route

          def getClientOperatorKeys200(responseClientKeys: ClientKeys)(implicit toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys]): Route =
            complete((200, responseClientKeys))
  def getClientOperatorKeys401(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((401, responseProblem))
  def getClientOperatorKeys404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((404, responseProblem))
        /**
           * Code: 200, Message: returns the corresponding array of keys, DataType: ClientKeys
   * Code: 401, Message: Unauthorized, DataType: Problem
   * Code: 404, Message: Client id not found, DataType: Problem
        */
        def getClientOperatorKeys(clientId: String, operatorId: String)
        (implicit contexts: Seq[(String, String)], toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route

          def getOperatorKeyById200(responseClientKey: ClientKey)(implicit toEntityMarshallerClientKey: ToEntityMarshaller[ClientKey]): Route =
            complete((200, responseClientKey))
  def getOperatorKeyById401(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((401, responseProblem))
  def getOperatorKeyById404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((404, responseProblem))
        /**
           * Code: 200, Message: returns the corresponding key, DataType: ClientKey
   * Code: 401, Message: Unauthorized, DataType: Problem
   * Code: 404, Message: Key not found, DataType: Problem
        */
        def getOperatorKeyById(operatorId: String, keyId: String)
        (implicit contexts: Seq[(String, String)], toEntityMarshallerClientKey: ToEntityMarshaller[ClientKey], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route

          def getOperatorKeys200(responseClientKeys: ClientKeys)(implicit toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys]): Route =
            complete((200, responseClientKeys))
  def getOperatorKeys401(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((401, responseProblem))
  def getOperatorKeys404(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((404, responseProblem))
        /**
           * Code: 200, Message: returns the corresponding array of keys, DataType: ClientKeys
   * Code: 401, Message: Unauthorized, DataType: Problem
   * Code: 404, Message: Client id not found, DataType: Problem
        */
        def getOperatorKeys(operatorId: String)
        (implicit contexts: Seq[(String, String)], toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route

    }

        trait OperatorApiMarshaller {
          implicit def fromEntityUnmarshallerOperatorKeySeedList: FromEntityUnmarshaller[Seq[OperatorKeySeed]]


        
          implicit def toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys]

  implicit def toEntityMarshallerClientKey: ToEntityMarshaller[ClientKey]

  implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem]

        }

