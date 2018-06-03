package io.callstats.event.user

import io.callstats.event.AuthenticatedEvent

/**
 * This is the first step to add a new participant to the list of conference participants
 * or start a new conference. If there are no participants in the given conference then
 * a new conference will be created with the conferenceID provided.
 */
class UserJoinEvent : AuthenticatedEvent() {
  override fun path(): String = ""
}