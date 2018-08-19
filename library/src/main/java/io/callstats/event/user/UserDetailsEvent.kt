package io.callstats.event.user

import io.callstats.event.SessionEvent

/**
 * You can submit the user details such as username using this event.
 */
internal class UserDetailsEvent(val userName: String) : SessionEvent() {
  override fun path() = "events/userdetails"
}