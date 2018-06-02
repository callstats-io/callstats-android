package io.callstats.event.info

/**
 * ICE candidate pair info for some event
 *
 * @param id as defined here: https://www.w3.org/TR/webrtc-stats/
 * @param localCandidateId Local candidate ID
 * @param remoteCandidateId Remote candidate ID
 * @param state "frozen", "waiting", "inprogress", "failed", "succeeded", "cancelled"
 * @param nominated nominated
 */
data class IceCandidatePair(
    val id: String,
    val localCandidateId: String,
    val remoteCandidateId: String,
    val state: String,
    val priority: Int,
    val nominated: Boolean)