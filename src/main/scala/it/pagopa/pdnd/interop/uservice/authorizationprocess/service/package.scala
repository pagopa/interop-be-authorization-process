package it.pagopa.pdnd.interop.uservice.authorizationprocess

import akka.actor.ActorSystem
import it.pagopa.interop.authorizationmanagement.client.model.Client
import it.pagopa.pdnd.interop.uservice._
import it.pagopa.interop.authorizationmanagement

package object service {
  type CatalogManagementInvoker       = catalogmanagement.client.invoker.ApiInvoker
  type AgreementManagementInvoker     = agreementmanagement.client.invoker.ApiInvoker
  type PartyManagementInvoker         = partymanagement.client.invoker.ApiInvoker
  type AuthorizationManagementInvoker = authorizationmanagement.client.invoker.ApiInvoker
  type UserRegistryManagementInvoker  = userregistrymanagement.client.invoker.ApiInvoker

  type ManagementClient = Client

  object AgreementManagementInvoker {
    def apply()(implicit actorSystem: ActorSystem): AgreementManagementInvoker =
      agreementmanagement.client.invoker.ApiInvoker(agreementmanagement.client.api.EnumsSerializers.all)
  }

  object CatalogManagementInvoker {
    def apply()(implicit actorSystem: ActorSystem): CatalogManagementInvoker =
      catalogmanagement.client.invoker.ApiInvoker(catalogmanagement.client.api.EnumsSerializers.all)
  }

  object PartyManagementInvoker {
    def apply()(implicit actorSystem: ActorSystem): PartyManagementInvoker =
      partymanagement.client.invoker.ApiInvoker(partymanagement.client.api.EnumsSerializers.all)
  }

  object AuthorizationManagementInvoker {
    def apply()(implicit actorSystem: ActorSystem): AuthorizationManagementInvoker =
      authorizationmanagement.client.invoker.ApiInvoker(authorizationmanagement.client.api.EnumsSerializers.all)
  }

  object UserRegistryManagementInvoker {
    def apply()(implicit actorSystem: ActorSystem): UserRegistryManagementInvoker =
      userregistrymanagement.client.invoker.ApiInvoker(userregistrymanagement.client.api.EnumsSerializers.all)
  }
}
