package io.callstats.event.stats

import io.callstats.event.SessionEvent

/**
 * All the conference stats inlcuding tracks, candidatePairs,trasnports, msts, dataChannels, codes and timestamps can be submitted using this event.
 * @param remoteID remote user identifier
 * @param connectionID Unique identifier of connection between two endpoints. This identifier should remain the same throughout the life-time of the connection.
 * @param stats An array of stats objects
 */
internal class ConferenceStats(
    val remoteID: String,
    val connectionID: String,
    val stats: Array<Any>) : SessionEvent()
{
  override fun url() = "https://stats.callstats.io"
  override fun path() = "stats"
}