package io.callstats

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