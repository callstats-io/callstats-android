package io.callstats.interceptor

import io.callstats.CallstatsWebRTCFunction
import io.callstats.OnIceConnectionChange
import io.callstats.event.Event
import io.callstats.event.ice.IceAbortedEvent
import io.callstats.event.ice.IceConnectionDisruptEndEvent
import io.callstats.event.ice.IceConnectionDisruptStartEvent
import io.callstats.event.ice.IceDisruptEndEvent
import io.callstats.event.ice.IceDisruptStartEvent
import io.callstats.event.ice.IceFailedEvent
import io.callstats.event.ice.IceRestartEvent
import io.callstats.event.ice.IceTerminatedEvent
import io.callstats.event.info.IceCandidatePair
import io.callstats.utils.candidatePairs
import io.callstats.utils.localCandidates
import io.callstats.utils.remoteCandidates
import org.webrtc.PeerConnection.IceConnectionState
import org.webrtc.PeerConnection.IceConnectionState.DISCONNECTED
import org.webrtc.PeerConnection.IceConnectionState.FAILED
import org.webrtc.PeerConnection.IceConnectionState.NEW
import org.webrtc.PeerConnection.IceConnectionState.CHECKING
import org.webrtc.PeerConnection.IceConnectionState.CLOSED
import org.webrtc.PeerConnection.IceConnectionState.CONNECTED
import org.webrtc.PeerConnection.IceConnectionState.COMPLETED
import org.webrtc.RTCStats

/**
 * Interceptor to handle ICE events
 */
internal class IceInterceptor : Interceptor {

  private var iceConnectionState = NEW
  private var iceCandidatePair: IceCandidatePair? = null
  private var timestamp = mutableMapOf<IceConnectionState, Long?>(NEW to System.currentTimeMillis())

  override fun process(
      webRTCEvent: CallstatsWebRTCFunction,
      remoteID: String,
      connectionID: String,
      stats: Map<String, RTCStats>): Array<Event>
  {
    // filter event
    if (webRTCEvent !is OnIceConnectionChange) {
      return emptyArray()
    }

    var events = emptyArray<Event>()
    val newState = webRTCEvent.state
    val newPair = stats.candidatePairs().firstOrNull()
    val newTimestamp = System.currentTimeMillis()

    // [ICE disruption start event]
    if (newState == DISCONNECTED && (iceConnectionState == CONNECTED || iceConnectionState == COMPLETED)) {
      if (newPair != null) {
        events += IceDisruptStartEvent(
            remoteID,
            connectionID,
            newPair,
            iceConnectionState.name.toLowerCase())
      }
    }

    // [ICE disruption end event]
    if (iceConnectionState == DISCONNECTED && (newState == CONNECTED || newState == COMPLETED || newState == CHECKING)) {
      val prevPair = iceCandidatePair
      val startTime = timestamp[DISCONNECTED]
      if (newPair != null && prevPair != null && startTime != null) {
        events += IceDisruptEndEvent(
            remoteID,
            connectionID,
            newPair,
            prevPair,
            newState.name.toLowerCase(),
            System.currentTimeMillis() - startTime)
      }
    }

    // [ICE restart event]
    if (newState == NEW) {
      val prevPair = iceCandidatePair
      if (prevPair != null) {
        events += IceRestartEvent(remoteID, connectionID, prevPair, iceConnectionState.name.toLowerCase())
      }
    }

    // [ICE failed event]
    if (newState == FAILED && (iceConnectionState == CHECKING || iceConnectionState == DISCONNECTED)) {
      val startTime = timestamp[iceConnectionState]
      if (startTime != null) {
        val pairs = stats.candidatePairs()
        val locals = stats.localCandidates()
        val remotes = stats.remoteCandidates()
        events += IceFailedEvent(
            remoteID,
            connectionID,
            iceConnectionState.name.toLowerCase(),
            newTimestamp - startTime)
            .apply {
              iceCandidatePairs.addAll(pairs)
              localIceCandidates.addAll(locals)
              remoteIceCandidates.addAll(remotes)
            }
      }
    }

    // [ICE aborted event]
    if (newState == CLOSED && (iceConnectionState == CHECKING || iceConnectionState == NEW)) {
      val startTime = timestamp[iceConnectionState]
      if (startTime != null) {
        val pairs = stats.candidatePairs()
        val locals = stats.localCandidates()
        val remotes = stats.remoteCandidates()
        events += IceAbortedEvent(
            remoteID,
            connectionID,
            iceConnectionState.name.toLowerCase(),
            newTimestamp - startTime)
            .apply {
              iceCandidatePairs.addAll(pairs)
              localIceCandidates.addAll(locals)
              remoteIceCandidates.addAll(remotes)
            }
      }
    }

    // [ICE terminated event]
    if (newState == CLOSED
        && (iceConnectionState == CONNECTED
            || iceConnectionState == COMPLETED
            || iceConnectionState == FAILED
            || iceConnectionState == DISCONNECTED))
    {
      val prevPair = iceCandidatePair
      if (prevPair != null) {
        events += IceTerminatedEvent(remoteID, connectionID, prevPair, iceConnectionState.name.toLowerCase())
      }
    }

    // [ICE connection disruption start]
    if (newState == DISCONNECTED && iceConnectionState == CHECKING) {
      events += IceConnectionDisruptStartEvent(remoteID, connectionID)
    }

    // [ICE connection disruption end]
    if (newState == CHECKING && iceConnectionState == DISCONNECTED) {
      val startTime = timestamp[iceConnectionState]
      if (startTime != null) {
        events += IceConnectionDisruptEndEvent(remoteID, connectionID, newTimestamp - startTime)
      }
    }

    // finally, update the states
    iceConnectionState = newState
    iceCandidatePair = newPair
    timestamp[newState] = newTimestamp

    return events
  }
}