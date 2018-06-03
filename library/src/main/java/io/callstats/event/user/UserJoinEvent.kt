package io.callstats.event.user

import io.callstats.event.AuthenticatedEvent
import io.callstats.event.CreateSessionEvent

/**
 * This is the first step to add a new participant to the list of conference participants
 * or start a new conference. If there are no participants in the given conference then
 * a new conference will be created with the conferenceID provided.
 */
class UserJoinEvent : AuthenticatedEvent(), CreateSessionEvent {
  override fun path(): String = ""
}