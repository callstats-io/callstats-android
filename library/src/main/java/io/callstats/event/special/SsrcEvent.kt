package io.callstats.event.special

import io.callstats.event.SessionEvent
import io.callstats.event.info.Ssrc

/**
 * Whenever a new media stream track appears,
 * for example a new participant joins or a new media source is added, the SSRC Map event MUST be sent.
 *
 * @param remoteID remote user identifier
 * @param connectionID Unique identifier of connection between two endpoints.
 */
internal class SsrcEvent(
    val remoteID: String,
    val connectionID: String) : SessionEvent()
{
  val ssrcData: MutableList<Ssrc> = mutableListOf()

  override fun path() = "events/ssrcmap"
}