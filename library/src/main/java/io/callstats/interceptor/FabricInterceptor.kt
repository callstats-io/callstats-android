package io.callstats.interceptor

import io.callstats.OnHold
import io.callstats.OnIceConnectionChange
import io.callstats.OnIceGatheringChange
import io.callstats.OnResume
import io.callstats.OnSignalingChange
import io.callstats.PeerEvent
import io.callstats.event.Event
import io.callstats.event.fabric.FabricActionEvent
import io.callstats.event.fabric.FabricDroppedEvent
import io.callstats.event.fabric.FabricSetupEvent
import io.callstats.event.fabric.FabricStateChangeEvent
import io.callstats.event.fabric.FabricTerminatedEvent
import io.callstats.event.fabric.FabricTransportChangeEvent
import io.callstats.event.info.IceCandidatePair
import io.callstats.utils.candidatePairs
import io.callstats.utils.localCandidates
import io.callstats.utils.remoteCandidates
import io.callstats.utils.selectedCandidatePairId
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceConnectionState
import org.webrtc.PeerConnection.IceConnectionState.DISCONNECTED
import org.webrtc.PeerConnection.IceConnectionState.FAILED
import org.webrtc.PeerConnection.IceConnectionState.NEW
import org.webrtc.PeerConnection.IceConnectionState.CLOSED
import org.webrtc.PeerConnection.IceConnectionState.CONNECTED
import org.webrtc.PeerConnection.IceConnectionState.COMPLETED
import org.webrtc.PeerConnection.IceGatheringState
import org.webrtc.PeerConnection.SignalingState
import org.webrtc.RTCStats

/**
 * Interceptor to handle fabric events
 */
internal class FabricInterceptor : Interceptor {

  private var iceConnectionState = IceConnectionState.NEW
  private var iceGatheringState = IceGatheringState.NEW
  private var signalingState = SignalingState.CLOSED
  private var timestamp = mutableMapOf<IceConnectionState, Long?>(NEW to System.currentTimeMillis())
  private var iceCandidatePair: IceCandidatePair? = null

  private var connected = false

  override fun process(
      connection: PeerConnection,
      event: PeerEvent,
      localID: String,
      remoteID: String,
      connectionID: String,
      stats: Map<String, RTCStats>): Array<Event>
  {
    // filter event
    if (event !is OnIceConnectionChange
        && event !is OnIceGatheringChange
        && event !is OnSignalingChange
        && event !is OnHold
        && event !is OnResume)
    {
      return emptyArray()
    }

    var events = emptyArray<Event>()
    val newTimestamp = System.currentTimeMillis()

    // [Fabric state change] send fabric change
    if ((event is OnIceConnectionChange && event.state != iceConnectionState)
            || (event is OnIceGatheringChange && event.state != iceGatheringState)
            || (event is OnSignalingChange && event.state != signalingState))
    {
      val prevState: String
      val newState: String
      val changedState: String

      when (event) {
        is OnIceConnectionChange -> {
          prevState = iceConnectionState.name
          newState = event.state.name
          changedState = "iceConnectionState"
        }
        is OnIceGatheringChange -> {
          prevState = iceGatheringState.name
          newState = event.state.name
          changedState = "iceGatheringState"
        }
        is OnSignalingChange -> {
          prevState = signalingState.name
          newState = event.state.name
          changedState = "signalingState"
        }
        else -> throw IllegalArgumentException()
      }

      events += FabricStateChangeEvent(
          remoteID = remoteID,
          connectionID = connectionID,
          prevState = prevState.toLowerCase().replace('_', '-'),
          newState = newState.toLowerCase().replace('_', '-'),
          changedState = changedState)
    }

    // [Fabric dropped] if connection was setup and ice state change from disconnect or complete to failed, send fabric drop
    if (connected
        && event is OnIceConnectionChange
        && event.state == FAILED
        && (iceConnectionState == COMPLETED || iceConnectionState == DISCONNECTED))
    {
      val startTime = timestamp[iceConnectionState]
      val pair = iceCandidatePair
      if (pair != null && startTime != null) {
        events += FabricDroppedEvent(
            remoteID = remoteID,
            connectionID = connectionID,
            prevIceConnectionState = iceConnectionState.name.toLowerCase(),
            delay = newTimestamp - startTime,
            currIceCandidatePair = pair
        )
      }
    }

    // [Fabric terminated] if connection was setup and ice state change to closed, send fabric terminated
    if (connected
        && event is OnIceConnectionChange
        && event.state == CLOSED
        && iceConnectionState != CLOSED)
    {
      events += FabricTerminatedEvent(remoteID, connectionID)
    }

    // when ICE state = CONNECTED
    if (event is OnIceConnectionChange && event.state == CONNECTED)
    {
      // get new pairs
      val selectedPairId = stats.selectedCandidatePairId()
      val pairs = stats.candidatePairs()
      val newPair = pairs.firstOrNull { it.id == selectedPairId }
      val locals = stats.localCandidates()
      val remotes = stats.remoteCandidates()

      // [Fabric setup] if never connect and connect, send fabric setup
      if (!connected) {
        connected = true

        val setupDelay = timestamp[NEW]?.let { newTimestamp - it } ?: 0
        events += FabricSetupEvent(remoteID, connectionID).apply {
          delay = setupDelay
          iceConnectivityDelay = delay
          iceCandidatePairs.addAll(pairs)
          localIceCandidates.addAll(locals)
          remoteIceCandidates.addAll(remotes)
          selectedCandidatePairID = selectedPairId
        }
      }
      // [Fabric transport change] if connect before, send transport change event
      else {
        val prevPair = iceCandidatePair
        val lastConnectDelay = timestamp[CONNECTED]?.let { newTimestamp - it } ?: 0
        if (prevPair != null && newPair != null) {
          events += FabricTransportChangeEvent(
              remoteID,
              connectionID,
              newPair,
              prevPair,
              event.state.name.toLowerCase(),
              iceConnectionState.name.toLowerCase(),
              lastConnectDelay).apply {
            localIceCandidates.addAll(locals)
            remoteIceCandidates.addAll(remotes)
          }
        }
      }

      // update current pair
      iceCandidatePair = newPair
    }

    // [Fabric action] hold and resume event from app
    if (connected) {
      when (event) {
        is OnHold -> events += FabricActionEvent(remoteID, connectionID, FabricActionEvent.EVENT_HOLD)
        is OnResume -> events += FabricActionEvent(remoteID, connectionID, FabricActionEvent.EVENT_RESUME)
      }
    }

    // finally, update the states
    when (event) {
      is OnIceConnectionChange -> {
        iceConnectionState = event.state
        timestamp[iceConnectionState] = newTimestamp
      }
      is OnIceGatheringChange -> iceGatheringState = event.state
      is OnSignalingChange -> signalingState = event.state
    }

    return events
  }
}