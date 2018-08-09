package io.callstats.interceptor

import io.callstats.CallstatsWebRTCFunction
import io.callstats.OnAddStream
import io.callstats.OnIceConnectionChange
import io.callstats.event.Event
import io.callstats.event.special.SsrcEvent
import io.callstats.utils.ssrcs
import org.webrtc.PeerConnection
import org.webrtc.RTCStats

/**
 * Interceptor to send SSRC map after connected
 */
internal class SsrcInterceptor : Interceptor {

  private var connected = false

  override fun process(
      connection: PeerConnection,
      webRTCEvent: CallstatsWebRTCFunction,
      localID: String,
      remoteID: String,
      connectionID: String,
      stats: Map<String, RTCStats>): Array<Event> {

    // only continue if the event is ice and stream added
    if (webRTCEvent !is OnIceConnectionChange && webRTCEvent !is OnAddStream) return emptyArray()

    // if event is ice connection change but already connected, do not send
    if (webRTCEvent is OnIceConnectionChange && connected) return emptyArray()

    // if event is ice connection change but not connect yet, set connected
    if (webRTCEvent is OnIceConnectionChange && webRTCEvent.state == PeerConnection.IceConnectionState.CONNECTED) {
      connected = true
    }

    val ssrcs = stats.ssrcs(connection, localID, remoteID)
    if (ssrcs.isNotEmpty()) {
      return arrayOf(SsrcEvent(remoteID, connectionID).apply { ssrcData.addAll(ssrcs) })
    }

    return emptyArray()
  }
}