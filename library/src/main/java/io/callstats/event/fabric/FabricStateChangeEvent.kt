package io.callstats.event.fabric

/**
 * Whenever the ICE connection state changes or ICE gathering state changes or signaling state changes then this event should be sent.
 *
 * @param remoteID remote user identifier
 * @param connectionID Unique identifier of connection between two endpoints. This identifier should remain the same throughout the life-time of the connection.
 * @param prevState Previous state
 * @param newState New state
 * @param changedState which kind of state changes "signalingState" or "connectionState" or "iceConnectionState" or "iceGatheringState"
 */
class FabricStateChangeEvent(
    val remoteID: String,
    val connectionID: String,
    val prevState: String,
    val newState: String,
    val changedState: String) : FabricEvent()