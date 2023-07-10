package it.pagopa.interop.authorizationprocess

import akka.actor.ActorSystem
import it.pagopa.interop.authorizationmanagement.client.model.Client
import it.pagopa.interop.selfcare.userregistry
import it.pagopa.interop._
import it.pagopa.interop.authorizationmanagement
import it.pagopa.interop.authorizationprocess.common.system.ApplicationConfiguration
import it.pagopa.interop.selfcare.partymanagement

import scala.concurrent.ExecutionContextExecutor

package object service {
  type PartyManagementInvoker         = partymanagement.client.invoker.ApiInvoker
  type AuthorizationManagementInvoker = authorizationmanagement.client.invoker.ApiInvoker
  type UserRegistryManagementInvoker  = userregistry.client.invoker.ApiInvoker

  type ManagementClient = Client

  type PartyManagementApiKeyValue = selfcare.partymanagement.client.invoker.ApiKeyValue

  object PartyManagementApiKeyValue {
    def apply(): PartyManagementApiKeyValue =
      partymanagement.client.invoker.ApiKeyValue(ApplicationConfiguration.partyManagementApiKey)
  }
  object PartyManagementInvoker     {
    def apply()(implicit actorSystem: ActorSystem): PartyManagementInvoker =
      partymanagement.client.invoker.ApiInvoker(partymanagement.client.api.EnumsSerializers.all)
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
