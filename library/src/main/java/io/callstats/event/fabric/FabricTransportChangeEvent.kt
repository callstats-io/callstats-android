package io.callstats.event.fabric

import io.callstats.event.info.IceCandidate
import io.callstats.event.info.IceCandidatePair

/**
 * Whenever the fabric transport changes this event should be called.
 *
 * @param remoteID remote user identifier
 * @param connectionID Unique identifier of connection between two endpoints.
 * @param currIceCandidatePair current [IceCandidatePair]
 * @param prevIceCandidatePair previous [IceCandidatePair]
 * @param delay delay
 */
internal class FabricTransportChangeEvent(
    val remoteID: String,
    val connectionID: String,
    val currIceCandidatePair: IceCandidatePair,
    val prevIceCandidatePair: IceCandidatePair,
    val currIceConnectionState: String,
    val prevIceConnectionState: String,
    val delay: Long) : FabricEvent() {

  val localIceCandidates: MutableList<IceCandidate> = mutableListOf()
  val remoteIceCandidates: MutableList<IceCandidate> = mutableListOf()

  /**
   * "turn/udp" "turn/tcp" "turn/tls"
   */
  var relayType: String? = null
}