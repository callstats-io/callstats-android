package io.callstats.event.ice

/**
 * When ICE connection disruption ends, this event should be submitted
 *
 * @param remoteID remote user identifier
 * @param connectionID Unique identifier of connection between two endpoints. This identifier should remain the same throughout the life-time of the connection.
 * @param delay delay in milliseconds (example: 3.5 seconds is 3500 in milliseconds)
 */
class IceConnectionDisruptEndEvent(
    val remoteID: String,
    val connectionID: String,
    val delay: Int) : IceEvent()
{
  val eventType = "iceConnectionDisruptionEnd"
  val currIceConnectionState = "checking"
  val prevIceConnectionState = "disconnected"
}