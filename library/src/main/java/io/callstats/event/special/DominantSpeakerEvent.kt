package io.callstats.event.special

import io.callstats.event.SessionEvent

/**
 * Using this event, you can specify the dominant speaker of the conference.
 * For reference you can check this link: http://www.sciencedirect.com/science/article/pii/S0885230812000186
 */
class DominantSpeakerEvent : SessionEvent() {
  override fun path() = "events/dominantspeaker"
}