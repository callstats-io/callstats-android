package io.callstats.event.special

import io.callstats.event.SessionEvent
import io.callstats.event.info.Feedback

/**
 * You can submit overall rating to conference and add comments as well.
 * It is also possible to give separate ratings for audio and video.
 *
 * @param feedback [Feedback] info
 */
class FeedbackEvent(val feedback: Feedback) : SessionEvent() {
  override fun path() = "events/feedback"
}