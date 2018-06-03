package io.callstats.event.fabric

import io.callstats.event.SessionEvent

/**
 * Base type for Fabric events
 */
abstract class FabricEvent : SessionEvent() {
  override fun path(): String = "fabric"
}