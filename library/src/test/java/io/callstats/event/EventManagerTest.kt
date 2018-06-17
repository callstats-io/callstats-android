package io.callstats.event

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.callstats.OnIceConnectionChange
import io.callstats.interceptor.Interceptor
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.webrtc.PeerConnection
import org.webrtc.RTCStatsCollectorCallback
import org.webrtc.RTCStatsReport

class EventManagerTest {

  @Mock private lateinit var mockInterceptor1: Interceptor
  @Mock private lateinit var mockInterceptor2: Interceptor
  @Mock private lateinit var sender: EventSender
  @Mock private lateinit var connection: PeerConnection

  private lateinit var manager: EventManager

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
    manager = EventManager(
        sender,
        "remote1",
        connection,
        arrayOf(mockInterceptor1, mockInterceptor2))

    whenever(connection.getStats(any())).thenAnswer {
      (it.arguments[0] as RTCStatsCollectorCallback).onStatsDelivered(RTCStatsReport(0, mapOf()))
    }
  }

  @Test
  fun forwardStatsToAllInterceptor() {
    manager.process(OnIceConnectionChange(PeerConnection.IceConnectionState.CONNECTED))
    verify(mockInterceptor1).process(any(), any(), any())
    verify(mockInterceptor2).process(any(), any(), any())
  }
}