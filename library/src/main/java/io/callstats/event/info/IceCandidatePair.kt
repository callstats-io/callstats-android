package io.callstats.event.info

import org.webrtc.RTCStats

/**
 * ICE candidate pair info for some event
 *
 * @param id as defined here: https://www.w3.org/TR/webrtc-stats/
 * @param localCandidateId Local candidate ID
 * @param remoteCandidateId Remote candidate ID
 * @param state "frozen", "waiting", "inprogress", "failed", "succeeded", "cancelled"
 * @param nominated nominated
 */
internal data class IceCandidatePair(
    val id: String,
    val localCandidateId: String,
    val remoteCandidateId: String,
    val state: String,
    val priority: Long,
    val nominated: Boolean) {

  companion object {
    fun fromStats(stats: RTCStats): IceCandidatePair {
      // callstats restful accept state "in-progress" with "inprogress"
      var state = stats.members["state"] as? String ?: ""
      if (state == "in-progress") state = "inprogress"

      return IceCandidatePair(
          id = stats.id,
          localCandidateId = stats.members["localCandidateId"] as? String ?: "",
          remoteCandidateId = stats.members["remoteCandidateId"] as? String ?: "",
          state = state,
          priority = stats.members["priority"] as? Long ?: 0,
          nominated = stats.members["nominated"] as? Boolean ?: false
      )
    }
  }
}