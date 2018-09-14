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

enum class CallstatsMediaType {
  VIDEO,
  AUDIO,
  SCREENSHARE
}
