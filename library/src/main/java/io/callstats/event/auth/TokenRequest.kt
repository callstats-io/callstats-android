package io.callstats.event.auth

import io.callstats.event.Event

class TokenRequest(val code: String, val clientID: String) : Event() {
  val grantType = "authorization_code"

  override fun url(): String = "https://auth.callstats.io"
  override fun path(): String = "authenticate"
}