package io.callstats.utils

import io.callstats.event.info.IceCandidate
import io.callstats.event.info.IceCandidatePair
import io.callstats.event.info.Ssrc
import org.webrtc.PeerConnection
import org.webrtc.RTCStats

typealias WebRTCStats = Map<String, RTCStats>

/**
 * Extract the selected candidate pair ID
 * @return selected candidate pair ID
 */
fun WebRTCStats.selectedCandidatePairId(): String? {
  return values
      .firstOrNull { it.type == "transport" }
      ?.members
      ?.get("selectedCandidatePairId") as? String
}

/**
 * Extract all candidate pairs from RTCStats
 * @return list of [IceCandidatePair]
 */
fun WebRTCStats.candidatePairs(): List<IceCandidatePair> {
  return values
      .filter { it.type == "candidate-pair" }
      .map { IceCandidatePair.fromStats(it) }
}

/**
 * Extract all local ICE candidate from RTCStats
 * @return list of [IceCandidate]
 */
fun WebRTCStats.localCandidates(): List<IceCandidate> {
  return values
      .filter { it.type == "local-candidate" }
      .map { IceCandidate.fromStats(it) }
}

/**
 * Extract all remote ICE candidate from RTCStats
 * @return list of [IceCandidate]
 */
fun WebRTCStats.remoteCandidates(): List<IceCandidate> {
  return values
      .filter { it.type == "remote-candidate" }
      .map { IceCandidate.fromStats(it) }
}

/**
 * Extract all SSRC details from stats and PeerConnection
 */
fun WebRTCStats.ssrcs(peerConnection: PeerConnection, localId: String, remoteId: String): List<Ssrc> {
  return values
      .filter { it.type == "inbound-rtp" || it.type == "outbound-rtp" }
      .mapNotNull { Ssrc.fromStats(it, peerConnection, localId, remoteId) }
}