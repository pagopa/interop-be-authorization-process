package it.pagopa.interop.authorizationprocess

import akka.actor.ActorSystem
import it.pagopa.interop.authorizationmanagement.client.model.Client
import it.pagopa.interop.selfcare.userregistry
import it.pagopa.interop._
import it.pagopa.interop.authorizationmanagement
import it.pagopa.interop.authorizationprocess.common.ApplicationConfiguration
import it.pagopa.interop.selfcare.partymanagement

import scala.concurrent.ExecutionContextExecutor

package object service {
  type CatalogManagementInvoker       = catalogmanagement.client.invoker.ApiInvoker
  type AgreementManagementInvoker     = agreementmanagement.client.invoker.ApiInvoker
  type PartyManagementInvoker         = partymanagement.client.invoker.ApiInvoker
  type PurposeManagementInvoker       = purposemanagement.client.invoker.ApiInvoker
  type AuthorizationManagementInvoker = authorizationmanagement.client.invoker.ApiInvoker
  type UserRegistryManagementInvoker  = userregistry.client.invoker.ApiInvoker

  type ManagementClient = Client

  type PartyManagementApiKeyValue = selfcare.partymanagement.client.invoker.ApiKeyValue

  object PartyManagementApiKeyValue {
    def apply(): PartyManagementApiKeyValue =
      partymanagement.client.invoker.ApiKeyValue(ApplicationConfiguration.partyManagementApiKey)
  }

  object AgreementManagementInvoker {
    def apply(blockingEc: ExecutionContextExecutor)(implicit actorSystem: ActorSystem): AgreementManagementInvoker =
      agreementmanagement.client.invoker.ApiInvoker(agreementmanagement.client.api.EnumsSerializers.all, blockingEc)
  }

  object CatalogManagementInvoker {
    def apply(blockingEc: ExecutionContextExecutor)(implicit actorSystem: ActorSystem): CatalogManagementInvoker =
      catalogmanagement.client.invoker.ApiInvoker(catalogmanagement.client.api.EnumsSerializers.all, blockingEc)
  }

  object PartyManagementInvoker {
    def apply()(implicit actorSystem: ActorSystem): PartyManagementInvoker =
      partymanagement.client.invoker.ApiInvoker(partymanagement.client.api.EnumsSerializers.all)
  }

  object PurposeManagementInvoker {
    def apply(blockingEc: ExecutionContextExecutor)(implicit actorSystem: ActorSystem): PurposeManagementInvoker =
      purposemanagement.client.invoker.ApiInvoker(purposemanagement.client.api.EnumsSerializers.all, blockingEc)
  }

  object AuthorizationManagementInvoker {
    def apply(blockingEc: ExecutionContextExecutor)(implicit actorSystem: ActorSystem): AuthorizationManagementInvoker =
      authorizationmanagement.client.invoker
        .ApiInvoker(authorizationmanagement.client.api.EnumsSerializers.all, blockingEc)
  }

  object UserRegistryManagementInvoker {
    def apply()(implicit actorSystem: ActorSystem): UserRegistryManagementInvoker =
      userregistry.client.invoker.ApiInvoker(userregistry.client.api.EnumsSerializers.all)
  }
}
