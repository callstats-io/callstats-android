package io.callstats.interceptor

import io.callstats.CallstatsMediaType
import io.callstats.MediaActionEvent
import io.callstats.MediaPlaybackEvent
import io.callstats.OnAudio
import io.callstats.OnPlaybackStalled
import io.callstats.OnPlaybackStart
import io.callstats.OnPlaybackSuspended
import io.callstats.OnScreenShare
import io.callstats.OnVideo
import io.callstats.OneWayMedia
import io.callstats.PeerEvent
import io.callstats.event.Event
import io.callstats.event.info.Ssrc
import io.callstats.utils.ssrcs
import org.webrtc.PeerConnection
import org.webrtc.RTCStats

private typealias ActionEvent = io.callstats.event.media.MediaActionEvent
private typealias PlaybackEvent = io.callstats.event.media.MediaPlaybackEvent

/**
 * Interceptor for media events
 */
internal class MediaInterceptor : Interceptor {

  override fun process(
      connection: PeerConnection,
      event: PeerEvent,
      localID: String,
      remoteID: String,
      connectionID: String,
      stats: Map<String, RTCStats>): Array<Event>
  {
    if (event !is MediaActionEvent && event !is MediaPlaybackEvent) return emptyArray()

    // [Media actions]
    if (event is MediaActionEvent) {
      val eventType = when (event) {
        is OnAudio -> if (event.mute) ActionEvent.EVENT_MUTE else ActionEvent.EVENT_UNMUTE
        is OnVideo -> if (event.enable) ActionEvent.EVENT_VIDEO_RESUME else ActionEvent.EVENT_VIDEO_PAUSE
        is OnScreenShare -> if (event.enable) ActionEvent.EVENT_SCREENSHARE_START else ActionEvent.EVENT_SCREENSHARE_STOP
      }
      return arrayOf(ActionEvent(remoteID, connectionID, eventType, event.mediaDeviceID))
    }
    // [Media playback]
    else if (event is MediaPlaybackEvent) {
      val evenType = when (event) {
        is OnPlaybackStart -> PlaybackEvent.EVENT_PLAYBACK_START
        is OnPlaybackSuspended -> PlaybackEvent.EVENT_PLAYBACK_SUSPENDED
        is OnPlaybackStalled -> PlaybackEvent.EVENT_PLAYBACK_STALLED
        is OneWayMedia -> PlaybackEvent.EVENT_ONE_WAY_MEDIA
      }
      val mediaType = when (event.mediaType) {
        CallstatsMediaType.VIDEO -> PlaybackEvent.MEDIA_VIDEO
        CallstatsMediaType.AUDIO -> PlaybackEvent.MEDIA_AUDIO
        CallstatsMediaType.SCREENSHARE -> PlaybackEvent.MEDIA_SCREEN
      }
      // get ssrc if need
      if (evenType == PlaybackEvent.EVENT_ONE_WAY_MEDIA) {
        return arrayOf(PlaybackEvent(remoteID, connectionID, evenType, mediaType, null))
      } else {
        val ssrc = stats.ssrcs(connection, localID, remoteID).firstOrNull { it.reportType == Ssrc.REPORT_LOCAL && it.mediaType == mediaType }
        if (ssrc != null) return arrayOf(PlaybackEvent(remoteID, connectionID, evenType, mediaType, ssrc.ssrc))
      }
    }

    return emptyArray()
  }
}