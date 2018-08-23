package io.callstats.interceptor

import io.callstats.PeerEvent
import io.callstats.event.Event
import org.webrtc.PeerConnection
import org.webrtc.RTCStats

/**
 * The interceptor to process the event sent by app
 */
internal interface Interceptor {

  /**
   * Process the incoming type and data stats
   */
  fun process(
      connection: PeerConnection,
      event: PeerEvent,
      localID: String,
      remoteID: String,
      connectionID: String,
      stats: Map<String, RTCStats>): Array<Event>
}