package it.pagopa.interop.authorizationprocess

import it.pagopa.interop.commons.utils.errors.ServiceCode

package object error {
  implicit val serviceCode: ServiceCode = ServiceCode("007")
}
