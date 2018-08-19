package io.callstats

import io.callstats.event.info.MediaDevice
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
sealed class WebRTCEvent {
  // public
  data class OnIceConnectionChange(val state: PeerConnection.IceConnectionState) : WebRTCEvent()
  data class OnIceGatheringChange(val state: PeerConnection.IceGatheringState) : WebRTCEvent()
  data class OnSignalingChange(val state: PeerConnection.SignalingState) : WebRTCEvent()
  class OnAddStream : WebRTCEvent()
  // internal
  internal class OnStats : WebRTCEvent()
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

/**
 * Application events
 */
sealed class ApplicationEvent
sealed class ApplicationPeerEvent(val remoteIDList: Array<String>): ApplicationEvent()
// app events
class OnDominantSpeaker : ApplicationEvent()
class OnDeviceConnected(val devices: Array<MediaDevice>) : ApplicationEvent()
class OnDeviceActive(val devices: Array<MediaDevice>) : ApplicationEvent()
// app peer events
class OnHold(remoteID: String): ApplicationPeerEvent(arrayOf(remoteID))
class OnResume(remoteID: String): ApplicationPeerEvent(arrayOf(remoteID))

// media
sealed class MediaActionEvent(val mediaDeviceID: String, remoteIDList: Array<String> = emptyArray()) : ApplicationPeerEvent(remoteIDList)
class OnAudio(val mute: Boolean, mediaDeviceID: String, remoteIDList: Array<String> = emptyArray()) : MediaActionEvent(mediaDeviceID, remoteIDList)
class OnVideo(val enable: Boolean, mediaDeviceID: String, remoteIDList: Array<String> = emptyArray()) : MediaActionEvent(mediaDeviceID, remoteIDList)
class OnScreenShare(val enable: Boolean, mediaDeviceID: String, remoteIDList: Array<String> = emptyArray()) : MediaActionEvent(mediaDeviceID, remoteIDList)
