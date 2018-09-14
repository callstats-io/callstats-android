package io.callstats

import io.callstats.event.info.MediaDevice
import org.webrtc.PeerConnection

/** base event for use with lib */
sealed class Event

/**
 * Events for specific peer that will be forwarded to lib
 */
sealed class PeerEvent : Event()

/**
 * App events that does not require peer
 */
sealed class AppEvent : Event()

// region WebRTC events
data class OnIceConnectionChange(val state: PeerConnection.IceConnectionState) : PeerEvent()
data class OnIceGatheringChange(val state: PeerConnection.IceGatheringState) : PeerEvent()
data class OnSignalingChange(val state: PeerConnection.SignalingState) : PeerEvent()
object OnAddStream : PeerEvent()
internal object OnStats : PeerEvent()
// endregion

// region Fabric events
object OnHold : PeerEvent()
object OnResume : PeerEvent()
// endregion

// region Device events
object OnDominantSpeaker : AppEvent()
class OnDeviceConnected(val devices: Array<MediaDevice>) : AppEvent()
class OnDeviceActive(val devices: Array<MediaDevice>) : AppEvent()
// endregion

// region Media action events
sealed class MediaActionEvent(val mediaDeviceID: String) : PeerEvent()
class OnAudio(val mute: Boolean, mediaDeviceID: String) : MediaActionEvent(mediaDeviceID)
class OnVideo(val enable: Boolean, mediaDeviceID: String) : MediaActionEvent(mediaDeviceID)
class OnScreenShare(val enable: Boolean, mediaDeviceID: String) : MediaActionEvent(mediaDeviceID)
// endregion

// region Media playback events
sealed class MediaPlaybackEvent(val mediaType: CallstatsMediaType) : PeerEvent()
class OnPlaybackStart(mediaType: CallstatsMediaType) : MediaPlaybackEvent(mediaType)
class OnPlaybackSuspended(mediaType: CallstatsMediaType) : MediaPlaybackEvent(mediaType)
class OnPlaybackStalled(mediaType: CallstatsMediaType) : MediaPlaybackEvent(mediaType)
class OneWayMedia(mediaType: CallstatsMediaType) : MediaPlaybackEvent(mediaType)
// endregion