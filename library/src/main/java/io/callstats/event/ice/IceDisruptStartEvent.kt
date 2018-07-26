package io.callstats.event.ice

import io.callstats.event.info.IceCandidatePair

/**
 * When ICE disruption starts, this event should be submitted
 *
 * @param remoteID remote user identifier
 * @param connectionID Unique identifier of connection between two endpoints. This identifier should remain the same throughout the life-time of the connection.
 * @param currIceCandidatePair current [IceCandidatePair]
 * @param prevIceConnectionState previous ice connection state "connected" or "completed"
 */
class IceDisruptStartEvent(
    val remoteID: String,
    val connectionID: String,
    val currIceCandidatePair: IceCandidatePair,
    val prevIceConnectionState: String) : IceEvent()
{
  val eventType = "iceDisruptionStart"
  val currIceConnectionState = "disconnected"
}