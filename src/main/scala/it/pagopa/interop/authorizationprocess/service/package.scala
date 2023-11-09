package it.pagopa.interop.authorizationprocess

import akka.actor.ActorSystem
import it.pagopa.interop.authorizationmanagement.client.model.Client
import it.pagopa.interop.authorizationmanagement
import it.pagopa.interop.authorizationprocess.common.system.ApplicationConfiguration
import it.pagopa.interop.selfcare.v2

import scala.concurrent.ExecutionContextExecutor

package object service {
  type AuthorizationManagementInvoker = authorizationmanagement.client.invoker.ApiInvoker
  type SelfcareV2Invoker              = v2.client.invoker.ApiInvoker
  type SelfcareV2ApiKeyValue          = v2.client.invoker.ApiKeyValue
  type ManagementClient               = Client

  object SelfcareV2ApiKeyValue {
    def apply(): SelfcareV2ApiKeyValue =
      v2.client.invoker.ApiKeyValue(ApplicationConfiguration.selfcareV2ApiKey)
  }

  object SelfcareV2Invoker {
    def apply()(implicit actorSystem: ActorSystem): SelfcareV2Invoker =
      v2.client.invoker.ApiInvoker(v2.client.api.EnumsSerializers.all)
  }

  object AuthorizationManagementInvoker {
    def apply(blockingEc: ExecutionContextExecutor)(implicit actorSystem: ActorSystem): AuthorizationManagementInvoker =
      authorizationmanagement.client.invoker
        .ApiInvoker(authorizationmanagement.client.api.EnumsSerializers.all, blockingEc)
  }
}
