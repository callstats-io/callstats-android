package io.callstats.event.info

/**
 * ICE candidate info
 */
data class IceCandidate(
    val id: String,
    val type: String,
    val ip: String,
    val port: Int,
    val candidateType: String,
    val transport: String)