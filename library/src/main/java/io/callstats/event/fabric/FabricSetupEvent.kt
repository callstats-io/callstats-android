package io.callstats.event.fabric

import io.callstats.event.info.IceCandidate
import io.callstats.event.info.IceCandidatePair

/**
 * it should be sent during initial fabric setup phase. After this connection is setup and you can send data
 *
 * @param remoteID remote user identifier
 * @param connectionID Unique identifier of connection between two endpoints.
 * This identifier should remain the same throughout the life-time of the connection.
 */
class FabricSetupEvent(
    val remoteID: String,
    val connectionID: String) : FabricEvent()
{
  /**
   * Total time to setup a conference for the participant.
   * The time when the user joins until the chosen candidate pair is connected (setup/failure)
   */
  var delay: Long? = null

  /**
   * The time taken for the ICE gathering to finish (ICE gathering state from new to complete)
   */
  var iceGatheringDelay: Long? = null

  /**
   * The time taken for the ICE to establish the connectivity (ICE connection state new to connected/completed)
   */
  var iceConnectivityDelay: Long? = null

  /**
   * Stream flow direction inside the fabric.
   * "sendonly", "receiveonly" or "sendrecv"
   * Default is ""sendrecv""
   */
  var fabricTransmissionDirection = "sendrecv"

  /**
   * Type of remote endpoint a fabric was established to.
   * "peer" or "server"
   * Default is "peer".
   */
  var remoteEndpointType = "peer"

  val localIceCandidates: List<IceCandidate>? = mutableListOf()
  val remoteIceCandidates: List<IceCandidate>? = mutableListOf()
  val iceCandidatePairs: List<IceCandidatePair>? = mutableListOf()
}