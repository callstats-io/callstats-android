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
 * WebRTC events that will be forwarded to callstats lib
 */
sealed class CallstatsWebRTCEvent
// public
data class OnIceConnectionChange(val state: PeerConnection.IceConnectionState): CallstatsWebRTCEvent()
data class OnIceGatheringChange(val state: PeerConnection.IceGatheringState): CallstatsWebRTCEvent()
data class OnSignalingChange(val state: PeerConnection.SignalingState): CallstatsWebRTCEvent()
class OnAddStream: CallstatsWebRTCEvent()
// internal
internal class OnStats: CallstatsWebRTCEvent()

/**
 * Logging level to use with [Callstats.log]
 */
enum class LoggingLevel {
  DEBUG, INFO, WARN, ERROR, FATAL
}

/**
 * Logging message type to use with [Callstats.log]
 */
enum class LoggingType {
  TEXT, JSON
}

/**
 * Application events
 */
sealed class CallstatsApplicationEvent
class OnHold: CallstatsApplicationEvent()
class OnResume: CallstatsApplicationEvent()