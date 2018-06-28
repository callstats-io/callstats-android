package io.callstats.interceptor

import io.callstats.CallstatsWebRTCFunction
import io.callstats.OnStats
import io.callstats.event.Event
import io.callstats.event.stats.ConferenceStats
import org.webrtc.RTCStats

/**
 * Interceptor to handle stats submission events
 */
class StatsInterceptor(private val remoteID: String): Interceptor {

  override fun process(webRTCEvent: CallstatsWebRTCFunction, connectionID: String, stats: Map<String, RTCStats>): Array<Event> {
    if (webRTCEvent !is OnStats) return emptyArray()
    return arrayOf(ConferenceStats(remoteID, connectionID, stats.values.toTypedArray()))
  }
}