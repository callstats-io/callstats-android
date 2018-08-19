package io.callstats.event.ice

import io.callstats.event.info.IceCandidate
import io.callstats.event.info.IceCandidatePair

/**
 * When ICE Aborts, this event should be submitted
 *
 * @param remoteID remote user identifier
 * @param connectionID Unique identifier of connection between two endpoints. This identifier should remain the same throughout the life-time of the connection.
 * @param prevIceConnectionState current ice connection state "checking" or "new"
 * @param delay delay in milliseconds (example: 3.5 seconds is 3500 in milliseconds)
 */
internal class IceAbortedEvent(
    val remoteID: String,
    val connectionID: String,
    val prevIceConnectionState: String,
    val delay: Long) : IceEvent()
{
  val eventType = "iceFailed"
  val currIceConnectionState = "closed"

  val localIceCandidates: MutableList<IceCandidate> = mutableListOf()
  val remoteIceCandidates: MutableList<IceCandidate> = mutableListOf()
  val iceCandidatePairs: MutableList<IceCandidatePair> = mutableListOf()
}