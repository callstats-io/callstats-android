package io.callstats.interceptor

import com.nhaarman.mockito_kotlin.whenever
import io.callstats.OnAddStream
import io.callstats.OnIceConnectionChange
import io.callstats.event.special.SsrcEvent
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.webrtc.PeerConnection
import org.webrtc.RTCStats
import org.webrtc.SessionDescription

class SsrcInterceptorTest {

  @Mock private lateinit var connection: PeerConnection

  private lateinit var interceptor: SsrcInterceptor

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
    interceptor = SsrcInterceptor()

    val ssrcString = """a=ssrc:1234 cname:4TOk42mSjXCkVIa6
a=ssrc:1234 msid:lgsCFqt9kN2fVKw5wg3NKqGdATQoltEwOdMS 35429d94-5637-4686-9ecd-7d0622261ce8
a=ssrc:1234 mslabel:lgsCFqt9kN2fVKw5wg3NKqGdATQoltEwOdMS
a=ssrc:1234 label:35429d94-5637-4686-9ecd-7d0622261ce8"""

    val sdp = SessionDescription(SessionDescription.Type.OFFER, ssrcString)
    whenever(connection.localDescription).thenReturn(sdp)
  }

  @Test
  fun onAddStreamSendEvent() {
    val stats = mapOf("a" to RTCStats(0, "inbound-rtp", "id1", mapOf("ssrc" to "1234", "isRemote" to false, "mediaType" to "audio")))
    val events = interceptor.process(connection, OnAddStream(), "local1", "remote1", "conn1", stats)
    assertTrue(events.any { it is SsrcEvent})
  }

  @Test
  fun onIceConnectedSendEvent() {
    val stats = mapOf("a" to RTCStats(0, "inbound-rtp", "id1", mapOf("ssrc" to "1234", "isRemote" to false, "mediaType" to "audio")))
    val events = interceptor.process(
        connection,
        OnIceConnectionChange(PeerConnection.IceConnectionState.CONNECTED),
        "local1",
        "remote1",
        "conn1",
        stats)
    assertTrue(events.any { it is SsrcEvent})
  }

  @Test
  fun onIceConnectedSecondTimeShouldNotSendEvent() {
    val stats = mapOf("a" to RTCStats(0, "inbound-rtp", "id1", mapOf("ssrc" to "1234", "isRemote" to false, "mediaType" to "audio")))
    interceptor.process(
        connection,
        OnIceConnectionChange(PeerConnection.IceConnectionState.CONNECTED),
        "local1",
        "remote1",
        "conn1",
        stats)
    val events = interceptor.process(
        connection,
        OnIceConnectionChange(PeerConnection.IceConnectionState.CONNECTED),
        "local1",
        "remote1",
        "conn1",
        stats)
    assertTrue(events.isEmpty())
  }
}