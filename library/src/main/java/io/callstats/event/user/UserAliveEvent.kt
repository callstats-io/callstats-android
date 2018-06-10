package io.callstats.event.user

import io.callstats.event.KeepAliveEvent

/**
 * UserAlive makes sure that the user is present in the conference.
 */
class UserAliveEvent : KeepAliveEvent() {
  override fun path(): String = "user/alive"
}