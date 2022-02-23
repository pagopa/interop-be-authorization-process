package it.pagopa.interop.authorizationprocess.util

import it.pagopa.interop.authorizationprocess.model.{ClientKey, ClientKeys}
import org.scalatest.matchers.{MatchResult, Matcher}

trait CustomMatchers {
  class ClientKeyMatcher(expectedKey: ClientKey) extends Matcher[ClientKey] {
    def apply(left: ClientKey) = {
      val clientKey = left.key
      MatchResult(
        clientKey == expectedKey.key,
        s"""Client key $clientKey is not equal to "${expectedKey.key}"""",
        s"""Client key $clientKey is equal to "${expectedKey.key}""""
      )
    }
  }

  class ClientKeysMatcher(expectedKeys: ClientKeys) extends Matcher[ClientKeys] {
    def apply(left: ClientKeys) = {
      val clientKeys = left.keys.map(_.key)
      MatchResult(
        clientKeys == expectedKeys.keys.map(_.key),
        s"""Client keys ${left.keys} are not equal to "${expectedKeys.keys}"""",
        s"""Client keys ${left.keys} are equal to "${expectedKeys.keys}""""
      )
    }
  }

  def haveTheSameKey(expectedKey: ClientKey)    = new ClientKeyMatcher(expectedKey)
  def haveTheSameKeys(expectedKeys: ClientKeys) = new ClientKeysMatcher(expectedKeys)
}

//selfless trait
object CustomMatchers extends CustomMatchers
