package io.callstats.event

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argWhere
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.callstats.CallstatsConfig
import io.callstats.WebRTCEvent.OnIceConnectionChange
import io.callstats.OnAudio
import io.callstats.OnHold
import io.callstats.OnResume
import io.callstats.OnScreenShare
import io.callstats.OnVideo
import io.callstats.event.fabric.FabricActionEvent
import io.callstats.event.media.MediaActionEvent
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

  private lateinit var manager: EventManagerImpl

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
    manager = EventManagerImpl(
        sender,
        "local1",
        "remote1",
        connection,
        CallstatsConfig(),
        arrayOf(mockInterceptor1, mockInterceptor2))
    manager.connectionID = "con1"

    whenever(connection.getStats(any())).thenAnswer {
      (it.arguments[0] as RTCStatsCollectorCallback).onStatsDelivered(RTCStatsReport(0, mapOf()))
    }

    whenever(mockInterceptor1.process(any(), any(), any(), any(), any(), any())).thenReturn(emptyArray())
    whenever(mockInterceptor2.process(any(), any(), any(), any(), any(), any())).thenReturn(emptyArray())
  }

  @Test
  fun forwardStatsToAllInterceptor() {
    manager.process(OnIceConnectionChange(PeerConnection.IceConnectionState.DISCONNECTED))
    verify(mockInterceptor1).process(any(), any(), any(), any(), any(), any())
    verify(mockInterceptor2).process(any(), any(), any(), any(), any(), any())
  }

  @Test
  fun processAppHoldAndResumeEvent() {
    manager.process(OnHold("remote1"))
    verify(sender).send(argWhere { it is FabricActionEvent && it.eventType == FabricActionEvent.EVENT_HOLD })
    manager.process(OnResume("remote1"))
    verify(sender).send(argWhere { it is FabricActionEvent && it.eventType == FabricActionEvent.EVENT_RESUME })
  }

  @Test
  fun processAppMediaActionEvent() {
    manager.process(OnAudio(true, "device1"))
    verify(sender).send(argWhere { it is MediaActionEvent && it.eventType == MediaActionEvent.EVENT_MUTE })
    manager.process(OnVideo(false, "device2"))
    verify(sender).send(argWhere { it is MediaActionEvent && it.eventType == MediaActionEvent.EVENT_VIDEO_PAUSE })
    manager.process(OnScreenShare(false, "device3"))
    verify(sender).send(argWhere { it is MediaActionEvent && it.eventType == MediaActionEvent.EVENT_SCREENSHARE_STOP })
  }
}