package io.callstats.event.ice

import io.callstats.event.info.IceCandidatePair

/**
 * When ICE disruption ends, this event should be submitted
 *
 * @param remoteID remote user identifier
 * @param connectionID Unique identifier of connection between two endpoints. This identifier should remain the same throughout the life-time of the connection.
 * @param currIceCandidatePair current [IceCandidatePair]
 * @param prevIceCandidatePair previous [IceCandidatePair]
 * @param currIceConnectionState current ice connection state "connected" or "completed" or "checking"
 * @param delay delay
 */
class IceDisruptEndEvent(
    val remoteID: String,
    val connectionID: String,
    val currIceCandidatePair: IceCandidatePair,
    val prevIceCandidatePair: IceCandidatePair,
    val currIceConnectionState: String,
    val delay: Long) : IceEvent()
{
  val eventType = "iceDisruptionEnd"
  val prevIceConnectionState = "disconnected"
}