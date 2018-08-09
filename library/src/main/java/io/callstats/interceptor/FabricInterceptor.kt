package io.callstats.interceptor

import io.callstats.CallstatsWebRTCFunction
import io.callstats.OnIceConnectionChange
import io.callstats.OnIceGatheringChange
import io.callstats.OnSignalingChange
import io.callstats.event.Event
import io.callstats.event.fabric.FabricDroppedEvent
import io.callstats.event.fabric.FabricSetupEvent
import io.callstats.event.fabric.FabricStateChangeEvent
import io.callstats.event.fabric.FabricTerminatedEvent
import io.callstats.utils.candidatePairs
import io.callstats.utils.localCandidates
import io.callstats.utils.remoteCandidates
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceConnectionState
import org.webrtc.PeerConnection.IceGatheringState
import org.webrtc.PeerConnection.SignalingState
import org.webrtc.RTCStats

/**
 * Interceptor to handle fabric events
 */
internal class FabricInterceptor : Interceptor {

  private var createTimestamp = System.currentTimeMillis()
  private var iceConnectionState = IceConnectionState.NEW
  private var iceGatheringState = IceGatheringState.NEW
  private var signalingState = SignalingState.CLOSED
  private var connected = false

  override fun process(
      connection: PeerConnection,
      webRTCEvent: CallstatsWebRTCFunction,
      localID: String,
      remoteID: String,
      connectionID: String,
      stats: Map<String, RTCStats>): Array<Event>
  {
    // filter event
    if (webRTCEvent !is OnIceConnectionChange
        && webRTCEvent !is OnIceGatheringChange
        && webRTCEvent !is OnSignalingChange)
    {
      return emptyArray()
    }

    var events = emptyArray<Event>()

    // [Fabric state change] if connection was setup, send fabric change
    if (connected
        && ((webRTCEvent is OnIceConnectionChange && webRTCEvent.state != iceConnectionState)
            || (webRTCEvent is OnIceGatheringChange && webRTCEvent.state != iceGatheringState)
            || (webRTCEvent is OnSignalingChange && webRTCEvent.state != signalingState)))
    {
      val prevState: String
      val newState: String
      val changedState: String

      when (webRTCEvent) {
        is OnIceConnectionChange -> {
          prevState = iceConnectionState.name
          newState = webRTCEvent.state.name
          changedState = "iceConnectionState"
        }
        is OnIceGatheringChange -> {
          prevState = iceGatheringState.name
          newState = webRTCEvent.state.name
          changedState = "iceGatheringState"
        }
        is OnSignalingChange -> {
          prevState = signalingState.name
          newState = webRTCEvent.state.name
          changedState = "signalingState"
        }
        else -> throw IllegalArgumentException()
      }

      events += FabricStateChangeEvent(
          remoteID = remoteID,
          connectionID = connectionID,
          prevState = prevState.toLowerCase(),
          newState = newState.toLowerCase(),
          changedState = changedState)
    }

    // [Fabric dropped] if connection was setup and ice state change from disconnect or complete to failed, send fabric drop
    if (connected
        && webRTCEvent is OnIceConnectionChange
        && webRTCEvent.state == IceConnectionState.FAILED
        && (iceConnectionState == IceConnectionState.COMPLETED || iceConnectionState == IceConnectionState.DISCONNECTED))
    {
      val pairs = stats.candidatePairs()

      if (pairs.isNotEmpty()) {
        events += FabricDroppedEvent(
            remoteID = remoteID,
            connectionID = connectionID,
            prevIceConnectionState = iceConnectionState.name.toLowerCase(),
            delay = 0,
            currIceCandidatePair = pairs[0]
        )
      }
    }

    // [Fabric terminated] if connection was setup and ice state change to closed, send fabric terminated
    if (connected
        && webRTCEvent is OnIceConnectionChange
        && webRTCEvent.state == IceConnectionState.CLOSED
        && iceConnectionState != IceConnectionState.CLOSED)
    {
      events += FabricTerminatedEvent(remoteID, connectionID)
    }

    // [Fabric setup] if never connect and connect, send fabric setup
    if (!connected
        && webRTCEvent is OnIceConnectionChange
        && webRTCEvent.state == IceConnectionState.CONNECTED)
    {
      connected = true

      // create event
      val pairs = stats.candidatePairs()
      val locals = stats.localCandidates()
      val remotes = stats.remoteCandidates()

      events += FabricSetupEvent(remoteID, connectionID).apply {
        delay = System.currentTimeMillis() - createTimestamp
        iceConnectivityDelay = delay
        iceCandidatePairs.addAll(pairs)
        localIceCandidates.addAll(locals)
        remoteIceCandidates.addAll(remotes)
      }
    }

    // finally, update the state
    when (webRTCEvent) {
      is OnIceConnectionChange -> iceConnectionState = webRTCEvent.state
      is OnIceGatheringChange -> iceGatheringState = webRTCEvent.state
      is OnSignalingChange -> signalingState = webRTCEvent.state
    }

    return events
  }
}