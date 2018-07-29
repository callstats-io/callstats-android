package io.callstats.event.ice

import io.callstats.event.SessionEvent

/**
 * Base type for ICE events
 */
abstract class IceEvent : SessionEvent() {
  override fun path() = "events/ice/status"
}