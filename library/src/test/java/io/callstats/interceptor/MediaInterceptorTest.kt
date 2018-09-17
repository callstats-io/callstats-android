package io.callstats.interceptor

import com.nhaarman.mockito_kotlin.whenever
import io.callstats.CallstatsMediaType
import io.callstats.OnAudio
import io.callstats.OnPlaybackStalled
import io.callstats.OnPlaybackStart
import io.callstats.OnPlaybackSuspended
import io.callstats.OnScreenShare
import io.callstats.OnVideo
import io.callstats.OneWayMedia
import io.callstats.event.media.MediaActionEvent
import io.callstats.event.media.MediaPlaybackEvent
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.webrtc.PeerConnection
import org.webrtc.RTCStats
import org.webrtc.SessionDescription

class MediaInterceptorTest {

  @Mock private lateinit var connection: PeerConnection

  private lateinit var interceptor: MediaInterceptor

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
    interceptor = MediaInterceptor()
  }

  @Test
  fun mediaActions() {
    val stats = mapOf<String, RTCStats>()
    val events = interceptor.process(connection, OnAudio(true, "device1"), "local1", "remote1", "conn1", stats)
    assertTrue(events.any { it is MediaActionEvent && it.eventType == MediaActionEvent.EVENT_MUTE })
    val events2 = interceptor.process(connection, OnVideo(true, "device1"), "local1", "remote1", "conn1", stats)
    assertTrue(events2.any { it is MediaActionEvent && it.eventType == MediaActionEvent.EVENT_VIDEO_RESUME })
    val events3 = interceptor.process(connection, OnScreenShare(true, "device1"), "local1", "remote1", "conn1", stats)
    assertTrue(events3.any { it is MediaActionEvent && it.eventType == MediaActionEvent.EVENT_SCREENSHARE_START })
  }

  @Test
  fun mediaPlayback() {
    val ssrcString = """a=ssrc:1234 cname:4TOk42mSjXCkVIa6
a=ssrc:1234 msid:lgsCFqt9kN2fVKw5wg3NKqGdATQoltEwOdMS 35429d94-5637-4686-9ecd-7d0622261ce8
a=ssrc:1234 mslabel:lgsCFqt9kN2fVKw5wg3NKqGdATQoltEwOdMS
a=ssrc:1234 label:35429d94-5637-4686-9ecd-7d0622261ce8"""

    val sdp = SessionDescription(SessionDescription.Type.OFFER, ssrcString)
    whenever(connection.localDescription).thenReturn(sdp)
    val stats = mapOf("a" to RTCStats(0, "inbound-rtp", "id1", mapOf("ssrc" to 1234L, "isRemote" to false, "mediaType" to "audio")))

    val events = interceptor.process(connection, OnPlaybackStart(CallstatsMediaType.AUDIO), "local1", "remote1", "conn1", stats)
    assertTrue(events.any { it is MediaPlaybackEvent && it.eventType == MediaPlaybackEvent.EVENT_PLAYBACK_START })
    val events2 = interceptor.process(connection, OnPlaybackStalled(CallstatsMediaType.AUDIO), "local1", "remote1", "conn1", stats)
    assertTrue(events2.any { it is MediaPlaybackEvent && it.eventType == MediaPlaybackEvent.EVENT_PLAYBACK_STALLED })
    val events3 = interceptor.process(connection, OnPlaybackSuspended(CallstatsMediaType.AUDIO), "local1", "remote1", "conn1", stats)
    assertTrue(events3.any { it is MediaPlaybackEvent && it.eventType == MediaPlaybackEvent.EVENT_PLAYBACK_SUSPENDED })
    val events4 = interceptor.process(connection, OneWayMedia(CallstatsMediaType.SCREENSHARE), "local1", "remote1", "conn1", stats)
    assertTrue(events4.any { it is MediaPlaybackEvent && it.eventType == MediaPlaybackEvent.EVENT_ONE_WAY_MEDIA })
  }
}