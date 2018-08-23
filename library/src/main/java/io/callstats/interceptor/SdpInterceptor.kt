package io.callstats.interceptor

import io.callstats.OnIceConnectionChange
import io.callstats.PeerEvent
import io.callstats.event.Event
import io.callstats.event.special.SdpEvent
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceConnectionState
import org.webrtc.RTCStats

/**
 * Interceptor to send sdp events
 */
internal class SdpInterceptor : Interceptor {

  private var connected = false

  override fun process(
      connection: PeerConnection,
      event: PeerEvent,
      localID: String,
      remoteID: String,
      connectionID: String,
      stats: Map<String, RTCStats>): Array<Event>
  {
    // send sdp after connected only once
    if (event is OnIceConnectionChange
        && event.state == IceConnectionState.CONNECTED
        && !connected) {
      connected = true

      return arrayOf(SdpEvent(remoteID, connectionID).apply {
        localSDP = connection.localDescription.description
        remoteSDP = connection.remoteDescription.description
      })
    }

    return emptyArray()
  }
}