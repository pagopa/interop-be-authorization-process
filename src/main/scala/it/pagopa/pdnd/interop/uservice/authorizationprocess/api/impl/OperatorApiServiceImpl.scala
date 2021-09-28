package it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import cats.implicits._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.OperatorApiService
import it.pagopa.pdnd.interop.uservice.authorizationprocess.error._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service._
import it.pagopa.pdnd.interop.uservice.keymanagement
import it.pagopa.pdnd.interop.uservice.keymanagement.client.invoker.{ApiError => AuthorizationManagementApiError}
import it.pagopa.pdnd.interop.uservice.partymanagement.client.model.{Problem => _, _}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.Product"))
class OperatorApiServiceImpl(
  authorizationManagementService: AuthorizationManagementService,
  agreementManagementService: AgreementManagementService,
  catalogManagementService: CatalogManagementService,
  partyManagementService: PartyManagementService
)(implicit ec: ExecutionContext)
    extends OperatorApiService {

  /** Code: 201, Message: Keys created, DataType: ClientKeys
    * Code: 400, Message: Bad Request, DataType: Problem
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 403, Message: Forbidden, DataType: Problem
    * Code: 404, Message: Client id not found, DataType: Problem
    */
  override def createOperatorKeys(taxCode: String, operatorKeySeeds: Seq[OperatorKeySeed])(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = ???
//  {
//    val result = for {
//      _          <- extractBearer(contexts)
//      _ <- operatorKeySeeds.traverse{seed => for {
//        clientUuid <- toUuid(seed.clientId).toFuture
//        client     <- authorizationManagementService.getClient(seed.clientId)
//        relationshipsIds <- securityOperatorRelationship(client.consumerId, taxCode)
//      } yield ???
//      }
//
//      seeds <- keysSeeds
//        .zip(relationshipsIds)
//        .traverse {
//          case (seed, Some(relationship)) =>
//            AuthorizationManagementService.toClientKeySeed(seed, relationship.id)
//          case (seed, None) =>
//            Left(SecurityOperatorRelationshipNotFound(client.consumerId.toString, seed.operatorTaxCode))
//        }
//        .toFuture
//      keysResponse <- authorizationManagementService.createKeys(clientUuid, seeds)
//    } yield ClientKeys(keysResponse.keys.map(AuthorizationManagementService.keyToApi))
//
//    onComplete(result) {
//      case Success(keys)                      => createKeys201(keys)
//      case Failure(ex @ UnauthenticatedError) => createKeys401(Problem(Option(ex.getMessage), 401, "Not authorized"))
//      case Failure(ex: EnumParameterError)    => createKeys400(Problem(Option(ex.getMessage), 400, "Bad Request"))
//      case Failure(ex: SecurityOperatorRelationshipNotFound) =>
//        createKeys403(Problem(Option(ex.getMessage), 403, "Forbidden"))
//      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
//        createKeys404(Problem(Some(ex.message), 404, "Not found"))
//      case Failure(ex) => createKeys500(Problem(Option(ex.getMessage), 500, "Error on key creation"))
//    }
//  }

  /** Code: 204, Message: the corresponding key has been deleted.
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Key not found, DataType: Problem
    */
  override def deleteOperatorKeyById(taxCode: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _ <- extractBearer(contexts)
      _ <- execForEachOperatorClient(taxCode, client => authorizationManagementService.deleteKey(client.id, keyId))
    } yield ()

    onComplete(result) {
      case Success(_) => deleteOperatorKeyById204
      case Failure(ex @ UnauthenticatedError) =>
        deleteOperatorKeyById401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        deleteOperatorKeyById404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(_ @NoResultsError) => deleteOperatorKeyById404(Problem(None, 404, "Not found"))
      case Failure(ex)                => complete((500, Problem(Option(ex.getMessage), 500, "Error on operator key delete")))
    }
  }

  /** Code: 204, Message: the corresponding key has been disabled.
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Key not found, DataType: Problem
    */
  override def disableOperatorKeyById(taxCode: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _ <- extractBearer(contexts)
      _ <- execForEachOperatorClient(taxCode, client => authorizationManagementService.disableKey(client.id, keyId))
    } yield ()

    onComplete(result) {
      case Success(_) => disableOperatorKeyById204
      case Failure(ex @ UnauthenticatedError) =>
        disableOperatorKeyById401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        disableOperatorKeyById404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(_ @NoResultsError) => deleteOperatorKeyById404(Problem(None, 404, "Not found"))
      case Failure(ex)                => complete((500, Problem(Option(ex.getMessage), 500, "Error on key disabling")))
    }
  }

  /** Code: 204, Message: the corresponding key has been enabled.
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Key not found, DataType: Problem
    */
  override def enableOperatorKeyById(taxCode: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _ <- extractBearer(contexts)
      _ <- execForEachOperatorClient(taxCode, client => authorizationManagementService.enableKey(client.id, keyId))
    } yield ()

    onComplete(result) {
      case Success(_) => enableOperatorKeyById204
      case Failure(ex @ UnauthenticatedError) =>
        enableOperatorKeyById401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        enableOperatorKeyById404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(_ @NoResultsError) => deleteOperatorKeyById404(Problem(None, 404, "Not found"))
      case Failure(ex)                => complete((500, Problem(Option(ex.getMessage), 500, "Error on key enabling")))
    }
  }

  /** Code: 200, Message: returns the corresponding key, DataType: ClientKey
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Key not found, DataType: Problem
    */
  override def getOperatorKeyById(taxCode: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientKey: ToEntityMarshaller[ClientKey],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _   <- extractBearer(contexts)
      key <- execForEachOperatorClient(taxCode, client => authorizationManagementService.getKey(client.id, keyId))
    } yield AuthorizationManagementService.keyToApi(key)

    onComplete(result) {
      case Success(result) => getOperatorKeyById200(result)
      case Failure(ex @ UnauthenticatedError) =>
        getOperatorKeyById401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        getOperatorKeyById404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(_ @NoResultsError) => deleteOperatorKeyById404(Problem(None, 404, "Not found"))
      case Failure(ex)                => complete((500, Problem(Option(ex.getMessage), 500, "Error on key retrieve")))
    }
  }

  /** Code: 200, Message: returns the corresponding array of keys, DataType: ClientKeys
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client id not found, DataType: Problem
    */
  override def getOperatorKeys(taxCode: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientKeys: ToEntityMarshaller[ClientKeys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _ <- extractBearer(contexts)
      keysResponse <- execForEachOperatorClient(
        taxCode,
        (client, operatorRelationships) =>
          for {
            clientKeys <- authorizationManagementService.getClientKeys(client.id)
            operatorKeys = clientKeys.keys.filter(key => operatorRelationships.items.exists(_.id == key.relationshipId))
          } yield keymanagement.client.model.KeysResponse(operatorKeys)
      )
    } yield ClientKeys(keysResponse.keys.map(AuthorizationManagementService.keyToApi))

    onComplete(result) {
      case Success(result) => getOperatorKeys200(result)
      case Failure(ex @ UnauthenticatedError) =>
        getOperatorKeys401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        getOperatorKeys404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(_ @NoResultsError) => deleteOperatorKeyById404(Problem(None, 404, "Not found"))
      case Failure(ex)                => complete((500, Problem(Option(ex.getMessage), 500, "Error on keys retrieve")))
    }
  }

  /** Exec f for each operator client, and returns the first successful operation, failure otherwise
    */
  private def execForEachOperatorClient[T](
    taxCode: String,
    f: (ManagementClient, Relationships) => Future[T]
  ): Future[T] = for {
    relationships <- partyManagementService.getRelationshipsByTaxCode(taxCode, None)
    clients <- relationships.items.flatTraverse(relationship =>
      authorizationManagementService.listClients(
        relationshipId = Some(relationship.id),
        offset = None,
        limit = None,
        eServiceId = None,
        consumerId = None
      )
    )
    recoverable <- clients.traverse(client => f(client, relationships).transform(Success(_)))
    success      = recoverable.collectFirst { case Success(result) => result }
    firstFailure = recoverable.collectFirst { case Failure(ex) => ex }
    result <- (success, firstFailure) match {
      case (Some(result), _) => Future.successful(result)
      case (None, Some(ex))  => Future.failed(ex)
      case (None, None)      => Future.failed(NoResultsError)
    }
  } yield result

  private def execForEachOperatorClient[T](taxCode: String, f: ManagementClient => Future[T]): Future[T] =
    execForEachOperatorClient(taxCode, (client, _) => f(client))

}
