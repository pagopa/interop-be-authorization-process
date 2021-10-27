package it.pagopa.pdnd.interop.uservice.authorizationprocess.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive1, Route}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
    import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
    import akka.http.scaladsl.unmarshalling.FromStringUnmarshaller
import it.pagopa.pdnd.interop.uservice.authorizationprocess.server.AkkaHttpHelper._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.KeysResponse
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model.Problem


    class WellKnownApi(
    wellKnownService: WellKnownApiService,
    wellKnownMarshaller: WellKnownApiMarshaller,
    wrappingDirective: Directive1[Seq[(String, String)]]
    ) {
    
    import wellKnownMarshaller._

    lazy val route: Route =
        path(".well-known" / "jwks.json") { 
        get { wrappingDirective { implicit contexts =>  
wellKnownService.getWellKnownKeys()



        }
        }
        }
    }


    trait WellKnownApiService {
          def getWellKnownKeys200(responseKeysResponse: KeysResponse)(implicit toEntityMarshallerKeysResponse: ToEntityMarshaller[KeysResponse]): Route =
            complete((200, responseKeysResponse))
  def getWellKnownKeys400(responseProblem: Problem)(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
            complete((400, responseProblem))
        /**
           * Code: 200, Message: PDND public keys in JWK format., DataType: KeysResponse
   * Code: 400, Message: Bad Request, DataType: Problem
        */
        def getWellKnownKeys()
        (implicit contexts: Seq[(String, String)], toEntityMarshallerKeysResponse: ToEntityMarshaller[KeysResponse], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route

    }

        trait WellKnownApiMarshaller {
        
        
          implicit def toEntityMarshallerKeysResponse: ToEntityMarshaller[KeysResponse]

  implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem]

        }

