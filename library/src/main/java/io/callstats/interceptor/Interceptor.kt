package io.callstats.interceptor

import io.callstats.CallstatsWebRTCFunction
import io.callstats.event.Event
import org.webrtc.RTCStats

/**
 * The interceptor to process the event sent by app
 */
internal interface Interceptor {

  /**
   * Process the incoming type and data stats
   */
  fun process(
      webRTCEvent: CallstatsWebRTCFunction,
      connectionID: String,
      stats: Map<String, RTCStats>): Array<Event>
}