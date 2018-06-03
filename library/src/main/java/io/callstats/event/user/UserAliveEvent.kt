package io.callstats.event.user

import io.callstats.event.SessionEvent

/**
 * UserAlive makes sure that the user is present in the conference.
 */
class UserAliveEvent : SessionEvent() {
  override fun path(): String = "user/alive"
}