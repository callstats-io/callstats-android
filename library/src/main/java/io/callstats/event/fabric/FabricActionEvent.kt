package io.callstats.event.fabric

/**
 * When the fabric hold or resume events happen, this event can be submitted
 *
 * @param remoteID remote user identifier
 * @param connectionID Unique identifier of connection between two endpoints. This identifier should remain the same throughout the life-time of the connection.
 * @param eventType Event Type either "fabricHold" or "fabricResume"
 */
class FabricActionEvent(
    val remoteID: String,
    val connectionID: String,
    val eventType: String) : FabricEvent()