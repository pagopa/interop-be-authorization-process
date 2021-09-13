package it.pagopa.pdnd.interop.uservice.authorizationprocess

import akka.actor.ActorSystem
import it.pagopa.pdnd.interop.uservice.keymanagement.client.api.EnumsSerializers
import it.pagopa.pdnd.interop.uservice.keymanagement.client.invoker.ApiInvoker
import it.pagopa.pdnd.interop.uservice.{catalogmanagement, keymanagement, agreementmanagement}

package object service {
  type KeyManagementInvoker           = ApiInvoker
  type AgreementManagementInvoker     = agreementmanagement.client.invoker.ApiInvoker
  type CatalogManagementInvoker       = catalogmanagement.client.invoker.ApiInvoker
  type AuthorizationManagementInvoker = keymanagement.client.invoker.ApiInvoker

  @SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
  object AgreementManagementInvoker {
    def apply()(implicit actorSystem: ActorSystem): AgreementManagementInvoker =
      agreementmanagement.client.invoker.ApiInvoker(agreementmanagement.client.api.EnumsSerializers.all)
  }

  @SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
  object CatalogManagementInvoker {
    def apply()(implicit actorSystem: ActorSystem): CatalogManagementInvoker =
      catalogmanagement.client.invoker.ApiInvoker(catalogmanagement.client.api.EnumsSerializers.all)
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
