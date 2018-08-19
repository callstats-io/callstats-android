package io.callstats.event.fabric

/**
 * it should be sent when fabric setup fails. This means connection has failed and you cannot send data
 *
 * @param reason "MediaConfigError", "MediaPermissionError", "MediaDeviceError", "NegotiationFailure",
 * "SDPGenerationError", "TransportFailure", "SignalingError", "IceConnectionFailure"
 * @param name Name
 * @param message Message
 */
internal class FabricSetupFailedEvent(val reason: String) : FabricEvent()
{
  var name: String? = null
  var message: String? = null

  /**
   * Stack trace of error
   */
  var stack: String? = null

  /**
   * Stream flow direction inside the fabric.
   * "sendonly", "receiveonly" or "sendrecv"
   * Default is ""sendrecv""
   */
  var fabricTransmissionDirection = "sendrecv"

  /**
   * Type of remote endpoint a fabric was established to.
   * "peer" or "server"
   * Default is "peer".
   */
  var remoteEndpointType = "peer"

  override fun path(): String = super.path() + "/setupfailed"
}