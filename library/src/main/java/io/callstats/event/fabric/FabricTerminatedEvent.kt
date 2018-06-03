package io.callstats.event.fabric

/**
 * it should be sent when fabric is terminated. This means connection has ended and you cannot send data
 *
 * @param remoteID remote user identifier
 * @param connectionID Unique identifier of connection between two endpoints. This identifier should remain the same throughout the life-time of the connection.
 */
class FabricTerminatedEvent(val remoteID: String, val connectionID: String) : FabricEvent() {
  override fun path(): String = super.path() + "/terminated"
}