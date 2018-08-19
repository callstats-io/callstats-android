package io.callstats.event.user

import io.callstats.event.KeepAliveEvent

/**
 * UserAlive makes sure that the user is present in the conference.
 */
internal class UserAliveEvent : KeepAliveEvent() {
  override fun path(): String = "events/user/alive"
}