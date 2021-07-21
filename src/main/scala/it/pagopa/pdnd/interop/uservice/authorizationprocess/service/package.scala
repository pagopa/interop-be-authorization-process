package it.pagopa.pdnd.interop.uservice.authorizationprocess

import akka.actor.ActorSystem
import it.pagopa.pdnd.interop.uservice.keymanagement.client.api.EnumsSerializers
import it.pagopa.pdnd.interop.uservice.keymanagement.client.invoker.ApiInvoker
import it.pagopa.pdnd.interop.uservice.{agreementprocess}

package object service {
  type KeyManagementInvoker    = ApiInvoker
  type AgreementProcessInvoker = agreementprocess.client.invoker.ApiInvoker

  @SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
  object AgreementProcessInvoker {
    def apply()(implicit actorSystem: ActorSystem): AgreementProcessInvoker =
      agreementprocess.client.invoker.ApiInvoker(agreementprocess.client.api.EnumsSerializers.all)
  }

  @SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
  object KeyManagementInvoker {
    def apply()(implicit actorSystem: ActorSystem): KeyManagementInvoker =
      ApiInvoker(EnumsSerializers.all)
  }
}
