package io.callstats.event.ice

/**
 * When ICE connection disruption starts, this event should be submitted
 *
 * @param remoteID remote user identifier
 * @param connectionID Unique identifier of connection between two endpoints. This identifier should remain the same throughout the life-time of the connection.
 */
class IceConnectionDisruptStartEvent(
    val remoteID: String,
    val connectionID: String) : IceEvent()
{
  val eventType = "iceConnectionDisruptionStart"
  val currIceConnectionState = "disconnected"
  val prevIceConnectionState = "checking"
}