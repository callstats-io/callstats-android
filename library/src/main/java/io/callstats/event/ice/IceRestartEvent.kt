package io.callstats.event.ice

import io.callstats.event.info.IceCandidatePair

/**
 * When ICE restarts, this event should be submitted
 *
 * @param remoteID remote user identifier
 * @param connectionID Unique identifier of connection between two endpoints. This identifier should remain the same throughout the life-time of the connection.
 * @param prevIceCandidatePair previous [IceCandidatePair]
 * @param prevIceConnectionState previous connection state "checking" or "connected" or "completed" or "failed" or "disconnected" or "closed"
 */
internal class IceRestartEvent(
    val remoteID: String,
    val connectionID: String,
    val prevIceCandidatePair: IceCandidatePair,
    val prevIceConnectionState: String) : IceEvent()
{
  val eventType = "iceRestarted"
  val currIceConnectionState = "new"
}