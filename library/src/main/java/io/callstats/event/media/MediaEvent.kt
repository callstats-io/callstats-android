package io.callstats.event.media

import io.callstats.event.SessionEvent

/**
 * Base class for media events
 */
abstract class MediaEvent : SessionEvent() {
  override fun path() = "events/media"
}