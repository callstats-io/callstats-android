package io.callstats.event.ice

import io.callstats.event.info.IceCandidate
import io.callstats.event.info.IceCandidatePair

/**
 * When ICE fails, this event should be submitted
 *
 * @param remoteID remote user identifier
 * @param connectionID Unique identifier of connection between two endpoints. This identifier should remain the same throughout the life-time of the connection.
 * @param localIceCandidates list of local [IceCandidate]
 * @param remoteIceCandidates list of remote [IceCandidate]
 * @param iceCandidatePairs list of [IceCandidatePair]
 * @param prevIceConnectionState current ice connection state "checking" or "disconnected"
 * @param delay delay in milliseconds (example: 3.5 seconds is 3500 in milliseconds)
 */
class IceFailedEvent(
    val remoteID: String,
    val connectionID: String,
    val localIceCandidates: Array<IceCandidate>,
    val remoteIceCandidates: Array<IceCandidate>,
    val iceCandidatePairs: Array<IceCandidatePair>,
    val prevIceConnectionState: String,
    val delay: Int) : IceEvent()
{
  val eventType = "iceFailed"
  val currIceConnectionState = "failed"
}