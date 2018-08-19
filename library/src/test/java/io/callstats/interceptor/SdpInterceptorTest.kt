package io.callstats.interceptor

import com.nhaarman.mockito_kotlin.whenever
import io.callstats.WebRTCEvent.OnIceConnectionChange
import io.callstats.event.special.SdpEvent
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceConnectionState
import org.webrtc.SessionDescription

class SdpInterceptorTest {

  @Mock private lateinit var connection: PeerConnection

  private lateinit var interceptor: SdpInterceptor

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
    interceptor = SdpInterceptor()
    whenever(connection.localDescription).thenReturn(SessionDescription(SessionDescription.Type.OFFER, "desc"))
    whenever(connection.remoteDescription).thenReturn(SessionDescription(SessionDescription.Type.ANSWER, "desc"))
  }

  @Test
  fun sendSdp() {
    val events = interceptor.process(
        connection,
        OnIceConnectionChange(IceConnectionState.CONNECTED),
        "local1",
        "remote1",
        "con1",
        mapOf())
    assertTrue(events.any { it is SdpEvent })
  }
}