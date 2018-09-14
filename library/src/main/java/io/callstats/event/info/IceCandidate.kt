package io.callstats.event.info

import org.webrtc.RTCStats

/**
 * ICE candidate info
 */
internal data class IceCandidate(
    val id: String,
    val type: String,
    val ip: String,
    val port: Int,
    val candidateType: String,
    val transport: String) {

  companion object {
    fun fromStats(stats: RTCStats): IceCandidate {
      return IceCandidate(
          id = stats.id,
          type = stats.type,
          ip = stats.members["ip"] as? String ?: "",
          port = stats.members["port"] as? Int ?: 0,
          candidateType = stats.members["candidateType"] as? String ?: "",
          transport = stats.members["protocol"] as? String ?: ""
      )
    }
  }
}