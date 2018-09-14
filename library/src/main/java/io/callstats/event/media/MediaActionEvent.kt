package io.callstats.event.media

/**
 * When a participant mutes/unmute the audio, pauses/resumes the video,
 * or starts/stops screen sharing, this event can be submitted
 *
 * @param remoteID server requires
 * @param connectionID Unique identifier of connection between two endpoints. This identifier should remain the same throughout the life-time of the connection.
 * @param eventType "audioMute" "audioUnmute" "screenShareStart" "screenShareStop" "videoPause" "videoResume"
 * @param mediaDeviceID Media Device ID
 */
internal class MediaActionEvent(
    val remoteID: String,
    val connectionID: String,
    val eventType: String,
    val mediaDeviceID: String) : MediaEvent() {

  companion object {
    const val EVENT_MUTE = "audioMute"
    const val EVENT_UNMUTE = "audioUnmute"
    const val EVENT_VIDEO_PAUSE = "videoPause"
    const val EVENT_VIDEO_RESUME = "videoResume"
    const val EVENT_SCREENSHARE_START = "screenShareStart"
    const val EVENT_SCREENSHARE_STOP = "screenShareStop"
  }

  // api required remoteIDList so use the one from the event
  val remoteIDList = arrayOf(remoteID)

  override fun path() = super.path() + "/actions"
}