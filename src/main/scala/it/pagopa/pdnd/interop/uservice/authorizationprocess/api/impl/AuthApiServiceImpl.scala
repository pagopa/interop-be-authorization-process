package it.pagopa.pdnd.interop.uservice.authorizationprocess.api.impl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route
import cats.implicits.toTraverseOps
import com.nimbusds.jose.JOSEException
import it.pagopa.pdnd.interop.uservice.agreementmanagement.client.invoker.{ApiError => AgreementManagementApiError}
import it.pagopa.pdnd.interop.uservice.keymanagement.client.invoker.{ApiError => AuthorizationManagementApiError}
import it.pagopa.pdnd.interop.uservice.keymanagement
import it.pagopa.pdnd.interop.uservice.authorizationprocess.api.AuthApiService
import it.pagopa.pdnd.interop.uservice.authorizationprocess.common.utils.{EitherOps, expireIn, toUuid}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.error.{
  AgreementStatusNotValidError,
  EnumParameterError,
  UnauthenticatedError,
  UuidConversionError
}
import it.pagopa.pdnd.interop.uservice.authorizationprocess.model._
import it.pagopa.pdnd.interop.uservice.authorizationprocess.service._

import java.text.ParseException
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@SuppressWarnings(Array("org.wartremover.warts.Any"))
class AuthApiServiceImpl(
  jwtValidator: JWTValidator,
  jwtGenerator: JWTGenerator,
  agreementProcessService: AgreementProcessService,
  agreementManagementService: AgreementManagementService,
  authorizationManagementService: AuthorizationManagementService
)(implicit ec: ExecutionContext)
    extends AuthApiService
    with Validation {

  /** Code: 200, Message: an Access token, DataType: ClientCredentialsResponse
    * Code: 403, Message: Unauthorized, DataType: Problem
    * Code: 400, Message: Bad request, DataType: Problem
    */

  override def createToken(accessTokenRequest: AccessTokenRequest)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerClientCredentialsResponse: ToEntityMarshaller[ClientCredentialsResponse],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {

    val token: Future[String] =
      for {
        bearerToken  <- extractBearer(contexts)
        validated    <- jwtValidator.validate(accessTokenRequest)
        pdndAudience <- agreementProcessService.retrieveAudience(bearerToken, accessTokenRequest.audience.toString)
        token        <- jwtGenerator.generate(validated, pdndAudience.audience.toList)
      } yield token

    onComplete(token) {
      case Success(tk) => createToken200(ClientCredentialsResponse(tk, "tokenType", expireIn))
      case Failure(ex) => manageError(ex)
    }
  }

  private def manageError(error: Throwable): Route = error match {
    case ex @ UnauthenticatedError => createToken401(Problem(Option(ex.getMessage), 401, "Not authorized"))
    case ex: ParseException        => createToken401(Problem(Option(ex.getMessage), 401, "Not authorized"))
    case ex: JOSEException         => createToken401(Problem(Option(ex.getMessage), 401, "Not authorized"))
    case ex                        => createToken400(Problem(Option(ex.getMessage), 400, "Something goes wrong during access token request"))
  }

  /** Code: 200, Message: Client created, DataType: Client
    * Code: 404, Message: Not Found, DataType: Problem
    * Code: 500, Message: Internal Server Error, DataType: Problem
    */
  override def createClient(clientSeed: ClientSeed)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route = {
    val result = for {
      bearerToken <- extractBearer(contexts)
      agreement   <- agreementManagementService.retrieveAgreement(bearerToken, clientSeed.agreementId.toString)
      _           <- validateUsableAgreementStatus(agreement)
      client      <- authorizationManagementService.createClient(clientSeed.agreementId, clientSeed.description)
    } yield Client(
      id = client.id,
      agreementId = client.agreementId,
      description = client.description,
      operators = client.operators
    )

    onComplete(result) {
      case Success(client)                    => createClient201(client)
      case Failure(ex @ UnauthenticatedError) => createClient401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AgreementStatusNotValidError) =>
        createClient422(Problem(Option(ex.getMessage), 422, "Invalid Agreement"))
      case Failure(ex: AgreementManagementApiError[_]) if ex.code == 404 =>
        createClient404(
          Problem(Some(s"Agreement id ${clientSeed.agreementId.toString} not found"), 404, "Agreement not found")
        )
      case Failure(ex) => createClient500(Problem(Option(ex.getMessage), 500, "Error on client creation"))
    }
  }

  /** Code: 200, Message: Client retrieved, DataType: Client
    * Code: 404, Message: Client not found, DataType: Problem
    * Code: 500, Message: Internal server error, DataType: Problem
    */
  override def getClient(clientId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route = {
    val result = for {
      _      <- extractBearer(contexts)
      client <- authorizationManagementService.getClient(clientId)
    } yield Client(
      id = client.id,
      agreementId = client.agreementId,
      description = client.description,
      operators = client.operators
    )

    onComplete(result) {
      case Success(client)                    => getClient200(client)
      case Failure(ex @ UnauthenticatedError) => getClient401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        getClient404(Problem(Some(ex.message), 404, "Client not found"))
      case Failure(ex) => getClient500(Problem(Option(ex.getMessage), 500, "Error on client retrieve"))
    }
  }

  /** Code: 200, Message: Request succeed, DataType: Seq[Client]
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 500, Message: Internal Server Error, DataType: Problem
    */
  override def listClients(
    offset: Option[Int],
    limit: Option[Int],
    agreementId: Option[String],
    operatorId: Option[String]
  )(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClientarray: ToEntityMarshaller[Seq[Client]]
  ): Route = {
    val result = for {
      _             <- extractBearer(contexts)
      agreementUuid <- agreementId.map(toUuid).sequence.toFuture
      operatorUuid  <- operatorId.map(toUuid).sequence.toFuture
      clients       <- authorizationManagementService.listClients(offset, limit, agreementUuid, operatorUuid)
    } yield clients.map(client =>
      Client(
        id = client.id,
        agreementId = client.agreementId,
        description = client.description,
        operators = client.operators
      )
    )

    onComplete(result) {
      case Success(clients)                   => listClients200(clients)
      case Failure(ex: UuidConversionError)   => listClients400(Problem(Option(ex.getMessage), 400, "Bad request"))
      case Failure(ex @ UnauthenticatedError) => listClients401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex)                        => listClients500(Problem(Option(ex.getMessage), 500, "Error on clients list"))
    }
  }

  /** Code: 204, Message: Client deleted
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client not found, DataType: Problem
    * Code: 500, Message: Internal server error, DataType: Problem
    */
  override def deleteClient(
    clientId: String
  )(implicit contexts: Seq[(String, String)], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route = {
    val result = for {
      _ <- extractBearer(contexts)
      _ <- authorizationManagementService.deleteClient(clientId)
    } yield ()

    onComplete(result) {
      case Success(_)                         => deleteClient204
      case Failure(ex @ UnauthenticatedError) => deleteClient401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        deleteClient404(Problem(Option(ex.getMessage), 404, "Client not found"))
      case Failure(ex) => deleteClient500(Problem(Option(ex.getMessage), 500, "Error on client deletion"))
    }
  }

  /** Code: 201, Message: Operator added, DataType: Client
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Missing Required Information, DataType: Problem
    * Code: 500, Message: Internal server error, DataType: Problem
    */
  override def addOperator(clientId: String, operatorSeed: OperatorSeed)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerClient: ToEntityMarshaller[Client]
  ): Route = {
    val result = for {
      _          <- extractBearer(contexts)
      clientUuid <- toUuid(clientId).toFuture
      client     <- authorizationManagementService.addOperator(clientUuid, operatorSeed.operatorId)
    } yield Client(
      id = client.id,
      agreementId = client.agreementId,
      description = client.description,
      operators = client.operators
    )

    onComplete(result) {
      case Success(client)                    => addOperator201(client)
      case Failure(ex @ UnauthenticatedError) => addOperator401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: UuidConversionError)   => addOperator400(Problem(Option(ex.getMessage), 400, "Bad request"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        addOperator404(Problem(Some(ex.message), 404, "Client not found"))
      case Failure(ex) => addOperator500(Problem(Option(ex.getMessage), 500, "Error on operator addition"))
    }
  }

  /** Code: 204, Message: Operator removed
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client or operator not found, DataType: Problem
    * Code: 500, Message: Internal server error, DataType: Problem
    */
  override def removeClientOperator(clientId: String, operatorId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _            <- extractBearer(contexts)
      clientUuid   <- toUuid(clientId).toFuture
      operatorUuid <- toUuid(operatorId).toFuture
      _            <- authorizationManagementService.removeClientOperator(clientUuid, operatorUuid)
    } yield ()

    onComplete(result) {
      case Success(_) => removeClientOperator204
      case Failure(ex @ UnauthenticatedError) =>
        removeClientOperator401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: UuidConversionError) =>
        removeClientOperator400(Problem(Option(ex.getMessage), 400, "Bad request"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        removeClientOperator404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(ex) => removeClientOperator500(Problem(Option(ex.getMessage), 500, "Error on operator removal"))
    }
  }

  /** Code: 200, Message: returns the corresponding key, DataType: Key
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Key not found, DataType: Problem
    * Code: 500, Message: Internal Server Error, DataType: Problem
    */
  override def getClientKeyById(clientId: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerKey: ToEntityMarshaller[Key],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _          <- extractBearer(contexts)
      clientUuid <- toUuid(clientId).toFuture
      key        <- authorizationManagementService.getKey(clientUuid, keyId)
    } yield keyToApi(key)

    onComplete(result) {
      case Success(key) => getClientKeyById200(key)
      case Failure(ex @ UnauthenticatedError) =>
        getClientKeyById401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        getClientKeyById404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(ex) => getClientKeyById500(Problem(Option(ex.getMessage), 500, "Error on key retrieve"))
    }
  }

  /** Code: 204, Message: the corresponding key has been deleted.
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Key not found, DataType: Problem
    * Code: 500, Message: Internal Server Error, DataType: Problem
    */
  override def deleteClientKeyById(clientId: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _          <- extractBearer(contexts)
      clientUuid <- toUuid(clientId).toFuture
      _          <- authorizationManagementService.deleteKey(clientUuid, keyId)
    } yield ()

    onComplete(result) {
      case Success(_) => deleteClientKeyById204
      case Failure(ex @ UnauthenticatedError) =>
        deleteClientKeyById401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        deleteClientKeyById404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(ex) => deleteClientKeyById500(Problem(Option(ex.getMessage), 500, "Error on key delete"))
    }
  }

  /** Code: 204, Message: the corresponding key has been enabled.
    * Code: 404, Message: Key not found, DataType: Problem
    */
  override def enableKeyById(clientId: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _          <- extractBearer(contexts)
      clientUuid <- toUuid(clientId).toFuture
      _          <- authorizationManagementService.enableKey(clientUuid, keyId)
    } yield ()

    onComplete(result) {
      case Success(_)                         => enableKeyById204
      case Failure(ex @ UnauthenticatedError) => enableKeyById401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        enableKeyById404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(ex) => enableKeyById500(Problem(Option(ex.getMessage), 500, "Error on key enabling"))
    }
  }

  /** Code: 204, Message: the corresponding key has been disabled.
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Key not found, DataType: Problem
    * Code: 500, Message: Internal Server Error, DataType: Problem
    */
  override def disableKeyById(clientId: String, keyId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _          <- extractBearer(contexts)
      clientUuid <- toUuid(clientId).toFuture
      _          <- authorizationManagementService.disableKey(clientUuid, keyId)
    } yield ()

    onComplete(result) {
      case Success(_) => disableKeyById204
      case Failure(ex @ UnauthenticatedError) =>
        disableKeyById401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        disableKeyById404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(ex) => disableKeyById500(Problem(Option(ex.getMessage), 500, "Error on key disabling"))
    }
  }

  /** Code: 201, Message: Keys created, DataType: Keys
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client id not found, DataType: Problem
    * Code: 500, Message: Internal Server Error, DataType: Problem
    */
  override def createKeys(clientId: String, keysSeeds: Seq[KeySeed])(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerKeys: ToEntityMarshaller[Keys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _            <- extractBearer(contexts)
      clientUuid   <- toUuid(clientId).toFuture
      seeds        <- keysSeeds.map(toClientKeySeed).sequence.toFuture
      keysResponse <- authorizationManagementService.createKeys(clientUuid, seeds)
    } yield Keys(keysResponse.keys.map(keyToApi))

    onComplete(result) {
      case Success(keys)                      => createKeys201(keys)
      case Failure(ex @ UnauthenticatedError) => createKeys401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: EnumParameterError)    => createKeys400(Problem(Option(ex.getMessage), 400, "Bad Request"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        createKeys404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(ex) => createKeys500(Problem(Option(ex.getMessage), 500, "Error on key creation"))
    }
  }

  /** Code: 200, Message: returns the corresponding array of keys, DataType: Keys
    * Code: 401, Message: Unauthorized, DataType: Problem
    * Code: 404, Message: Client id not found, DataType: Problem
    * Code: 500, Message: Internal Server Error, DataType: Problem
    */
  override def getClientKeys(clientId: String)(implicit
    contexts: Seq[(String, String)],
    toEntityMarshallerKeys: ToEntityMarshaller[Keys],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem]
  ): Route = {
    val result = for {
      _            <- extractBearer(contexts)
      clientUuid   <- toUuid(clientId).toFuture
      keysResponse <- authorizationManagementService.getClientKeys(clientUuid)
    } yield Keys(keysResponse.keys.map(keyToApi))

    onComplete(result) {
      case Success(keys)                      => getClientKeys200(keys)
      case Failure(ex @ UnauthenticatedError) => getClientKeys401(Problem(Option(ex.getMessage), 401, "Not authorized"))
      case Failure(ex: AuthorizationManagementApiError[_]) if ex.code == 404 =>
        getClientKeys404(Problem(Some(ex.message), 404, "Not found"))
      case Failure(ex) => getClientKeys500(Problem(Option(ex.getMessage), 500, "Error on client keys retrieve"))
    }
  }

  private[this] def keyToApi(key: keymanagement.client.model.Key): Key =
    Key(
      kty = key.kty,
      key_ops = key.keyOps,
      use = key.use,
      alg = key.alg,
      kid = key.kid,
      x5u = key.x5u,
      x5t = key.x5t,
      x5tS256 = key.x5tS256,
      x5c = key.x5c,
      crv = key.crv,
      x = key.x,
      y = key.y,
      d = key.d,
      k = key.k,
      n = key.n,
      e = key.e,
      p = key.p,
      q = key.q,
      dp = key.dp,
      dq = key.dq,
      qi = key.qi,
      oth = key.oth.map(_.map(primeInfoToApi))
    )

  private[this] def primeInfoToApi(info: keymanagement.client.model.OtherPrimeInfo): OtherPrimeInfo =
    OtherPrimeInfo(r = info.r, d = info.d, t = info.t)

  private[this] def toClientKeySeed(keySeed: KeySeed): Either[EnumParameterError, keymanagement.client.model.KeySeed] =
    Try(keymanagement.client.model.KeySeedEnums.Use.withName(keySeed.use)).toEither
      .map(use =>
        keymanagement.client.model
          .KeySeed(operatorId = keySeed.operatorId, key = keySeed.key, use = use, alg = keySeed.alg)
      )
      .left
      .map(_ => EnumParameterError("use", keymanagement.client.model.KeySeedEnums.Use.values.toSeq.map(_.toString)))
}
