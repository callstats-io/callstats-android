package io.callstats.event.media

import io.callstats.event.SessionEvent

/**
 * Base class for media events
 */
internal abstract class MediaEvent : SessionEvent() {
  override fun path() = "events/media"
}