package it.pagopa.pdnd.interop.uservice.authorizationprocess

import akka.actor.ActorSystem
import it.pagopa.pdnd.interop.uservice.keymanagement.client.api.EnumsSerializers
import it.pagopa.pdnd.interop.uservice.keymanagement.client.invoker.ApiInvoker
import it.pagopa.pdnd.interop.uservice.{agreementprocess, keymanagement, partymanagement}
import it.pagopa.pdnd.interopuservice.catalogprocess

package object service {
  type KeyManagementInvoker           = ApiInvoker
  type AgreementProcessInvoker        = agreementprocess.client.invoker.ApiInvoker
  type CatalogProcessInvoker          = catalogprocess.client.invoker.ApiInvoker
  type PartyManagementInvoker         = partymanagement.client.invoker.ApiInvoker
  type AuthorizationManagementInvoker = keymanagement.client.invoker.ApiInvoker

  @SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
  object AgreementProcessInvoker {
    def apply()(implicit actorSystem: ActorSystem): AgreementProcessInvoker =
      agreementprocess.client.invoker.ApiInvoker(agreementprocess.client.api.EnumsSerializers.all)
  }

  @SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
  object CatalogProcessInvoker {
    def apply()(implicit actorSystem: ActorSystem): CatalogProcessInvoker =
      catalogprocess.client.invoker.ApiInvoker(catalogprocess.client.api.EnumsSerializers.all)
  }

  @SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
  object PartyManagementInvoker {
    def apply()(implicit actorSystem: ActorSystem): PartyManagementInvoker =
      partymanagement.client.invoker.ApiInvoker(partymanagement.client.api.EnumsSerializers.all)
  }

  @SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
  object AuthorizationManagementInvoker {
    def apply()(implicit actorSystem: ActorSystem): AuthorizationManagementInvoker =
      keymanagement.client.invoker.ApiInvoker(keymanagement.client.api.EnumsSerializers.all)
  }

  @SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
  object KeyManagementInvoker {
    def apply()(implicit actorSystem: ActorSystem): KeyManagementInvoker =
      ApiInvoker(EnumsSerializers.all)
  }
}
