package io.callstats.event.user

import io.callstats.event.AuthenticatedEvent
import io.callstats.event.CreateSessionEvent
import io.callstats.event.info.EndpointInfo

/**
 * This is the first step to add a new participant to the list of conference participants
 * or start a new conference. If there are no participants in the given conference then
 * a new conference will be created with the conferenceID provided.
 */
class UserJoinEvent(appVersion: String? = null) : AuthenticatedEvent(), CreateSessionEvent {

  val endpointInfo = EndpointInfo(appVersion)

  override fun path(): String = ""
}