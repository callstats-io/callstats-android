package io.callstats.event.fabric

import io.callstats.event.info.IceCandidatePair

/**
 * Whenever the fabric is dropped, this should be notified.
 *
 * @param remoteID remote user identifier
 * @param connectionID Unique identifier of connection between two endpoints. This identifier should remain the same throughout the life-time of the connection.
 * @param currIceCandidatePair current [IceCandidatePair]
 * @param prevIceConnectionState previous ice connection state "disconnected" or "completed"
 * @param delay delay
 */
class FabricDroppedEvent(
    val remoteID: String,
    val connectionID: String,
    val currIceCandidatePair: IceCandidatePair,
    val prevIceConnectionState: String,
    val delay: Int) : FabricEvent()
{
  val currIceConnectionState = "failed"

  override fun path(): String = super.path() + "/status"
}