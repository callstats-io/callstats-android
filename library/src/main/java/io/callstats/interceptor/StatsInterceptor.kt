package io.callstats.interceptor

import io.callstats.OnStats
import io.callstats.PeerEvent
import io.callstats.event.Event
import io.callstats.event.stats.ConferenceStats
import org.webrtc.PeerConnection
import org.webrtc.RTCStats

/**
 * Interceptor to handle stats submission events
 */
internal class StatsInterceptor : Interceptor {

  override fun process(
      connection: PeerConnection,
      event: PeerEvent,
      localID: String,
      remoteID: String,
      connectionID: String,
      stats: Map<String, RTCStats>): Array<Event>
  {
    if (event !is OnStats) return emptyArray()
    return arrayOf(ConferenceStats(remoteID, connectionID, stats.values.toTypedArray()))
  }
}