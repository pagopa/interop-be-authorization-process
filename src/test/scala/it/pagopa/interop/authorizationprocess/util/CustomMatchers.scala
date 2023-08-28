package it.pagopa.interop.authorizationprocess.util

import it.pagopa.interop.authorizationprocess.model.{Key, Keys}
import org.scalatest.matchers.{MatchResult, Matcher}

trait CustomMatchers {
  class KeyMatcher(expectedKey: Key) extends Matcher[Key] {
    def apply(left: Key) = {
      val clientKey = left.encodedPem
      MatchResult(
        clientKey == expectedKey.encodedPem,
        s"""Client key $clientKey is not equal to "${expectedKey.encodedPem}"""",
        s"""Client key $clientKey is equal to "${expectedKey.encodedPem}""""
      )
    }
  }

  class KeysMatcher(expectedKeys: Keys) extends Matcher[Keys] {
    def apply(left: Keys) = {
      val clientKeys = left.keys.map(_.encodedPem)
      MatchResult(
        clientKeys == expectedKeys.keys.map(_.encodedPem),
        s"""Client keys ${left.keys} are not equal to "${expectedKeys.keys}"""",
        s"""Client keys ${left.keys} are equal to "${expectedKeys.keys}""""
      )
    }
  }

  def haveTheSameKey(expectedKey: Key)    = new KeyMatcher(expectedKey)
  def haveTheSameKeys(expectedKeys: Keys) = new KeysMatcher(expectedKeys)
}

//selfless trait
object CustomMatchers extends CustomMatchers
