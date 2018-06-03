package io.callstats.event.user

import io.callstats.event.SessionEvent

/**
 * User left event should be sent when a user leaves the conference.
 */
class UserLeftEvent : SessionEvent() {
  override fun path(): String = "user/left"
}