package io.callstats.event.media

/**
 * When the media playback starts, suspended or stalls, this event can be submitted
 *
 * @param remoteID server requires
 * @param connectionID Unique identifier of connection between two endpoints. This identifier should remain the same throughout the life-time of the connection.
 * @param eventType "mediaPlaybackStart" "mediaPlaybackSuspended" "mediaPlaybackStalled" "oneWayMedia"
 * @param mediaType Media type "audio" "video" "screen"
 */
internal class MediaPlaybackEvent(
    val remoteID: String,
    val connectionID: String,
    val eventType: String,
    val mediaType: String,
    val ssrc: String?) : MediaEvent() {

  companion object {
    const val EVENT_PLAYBACK_START = "mediaPlaybackStart"
    const val EVENT_PLAYBACK_SUSPENDED = "mediaPlaybackSuspended"
    const val EVENT_PLAYBACK_STALLED = "mediaPlaybackStalled"
    const val EVENT_ONE_WAY_MEDIA = "oneWayMedia"
    const val MEDIA_VIDEO = "video"
    const val MEDIA_AUDIO = "audio"
    const val MEDIA_SCREEN = "screen"
  }
}