package io.callstats

import org.webrtc.PeerConnection

/**
 * Error type to report to Callstats
 */
enum class CallstatsError(val value: String) {

  /**
   * The failure occurred because permission
   */
  MEDIA_PERMISSION("MediaPermissionError"),

  /**
   * The failure occurred in createOffer/createAnswer function.
   */
  SDP_GENERATION("SDPGenerationError"),

  /**
   * The failure occurred in setLocalDescription, setRemoteDescription function.
   */
  NEGOTIATION("NegotiationFailure"),

  /**
   * Signaling related errors in the application.
   */
  SIGNALING("SignalingError")
}

/**
 *
 */
sealed class CallstatsWebRTCFunction
// public
data class OnIceConnectionChange(val state: PeerConnection.IceConnectionState): CallstatsWebRTCFunction()
data class OnIceGatheringChange(val state: PeerConnection.IceGatheringState): CallstatsWebRTCFunction()
data class OnSignalingChange(val state: PeerConnection.SignalingState): CallstatsWebRTCFunction()
// internal
internal class OnStats: CallstatsWebRTCFunction()