package it.pagopa.pdnd.interop.uservice.authorizationprocess.error

final case class SecurityOperatorRelationshipNotFound(consumerId: String, operatorTaxCode: String)
    extends Throwable(
      s"Security operator relationship not found for consumer $consumerId and tax code $operatorTaxCode"
    )
