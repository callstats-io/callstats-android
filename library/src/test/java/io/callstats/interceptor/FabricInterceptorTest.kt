package io.callstats.interceptor

import io.callstats.OnHold
import io.callstats.OnIceConnectionChange
import io.callstats.OnResume
import io.callstats.event.fabric.FabricActionEvent
import io.callstats.event.fabric.FabricSetupEvent
import io.callstats.event.fabric.FabricStateChangeEvent
import io.callstats.event.fabric.FabricTransportChangeEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceConnectionState
import org.webrtc.RTCStats

class FabricInterceptorTest {

  @Mock private lateinit var connection: PeerConnection

  private lateinit var interceptor: FabricInterceptor

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
    interceptor = FabricInterceptor()
  }

  @Test
  fun fabricConnectSend() {
    val stats = mapOf<String, RTCStats>()
    val events = interceptor.process(connection, OnIceConnectionChange(IceConnectionState.CONNECTED), "local1", "remote1", "con1", stats)
    assertEquals(2, events.size) // include state change
    assertTrue(events.any { it is FabricSetupEvent })
  }

  @Test
  fun fabricConnectOnlyFirstTime() {
    connected()
    val stats = mapOf<String, RTCStats>()
    val events = interceptor.process(connection, OnIceConnectionChange(IceConnectionState.CONNECTED), "local1", "remote1", "con1", stats)
    assertEquals(0, events.size)
    interceptor.process(connection, OnIceConnectionChange(IceConnectionState.FAILED), "local1", "remote1", "con1", stats)
    val events2 = interceptor.process(connection, OnIceConnectionChange(IceConnectionState.CONNECTED), "local1", "remote1", "con1", stats)
    assertEquals(1, events2.size)
    assertFalse(events2.first() is FabricSetupEvent)
  }

  @Test
  fun fabricTransportChange() {
    connected()
    val stats = mapOf(
        "local" to RTCStats(0, "local-candidate", "local", mapOf()),
        "remote" to RTCStats(0, "remote-candidate", "remote", mapOf()),
        "pair" to RTCStats(0, "candidate-pair", "pair", mapOf()),
        "transport" to RTCStats(0, "transport", "transport", mapOf("selectedCandidatePairId" to "pair")))
    val events = interceptor.process(connection, OnIceConnectionChange(IceConnectionState.CONNECTED), "local1", "remote1", "con1", stats)
    assertEquals(1, events.size)
    assertTrue(events.first() is FabricTransportChangeEvent)
  }

  @Test
  fun fabricStateSend() {
    val stats = mapOf<String, RTCStats>()
    val events = interceptor.process(connection, OnIceConnectionChange(IceConnectionState.CHECKING), "local1", "remote1", "con1", stats)
    assertEquals(1, events.size)
    assertTrue(events.first() is FabricStateChangeEvent)
  }

  @Test
  fun fabricActions() {
    connected()
    val stats = mapOf<String, RTCStats>()
    val events = interceptor.process(connection, OnHold, "local1", "remote1", "con1", stats)
    assertEquals(1, events.size)
    assertTrue(events.first() is FabricActionEvent)
    val events2 = interceptor.process(connection, OnResume, "local1", "remote1", "con1", stats)
    assertEquals(1, events2.size)
    assertTrue(events2.first() is FabricActionEvent)
  }

  // utils

  private fun connected() {
    val stats = mapOf(
        "local" to RTCStats(0, "local-candidate", "local", mapOf()),
        "remote" to RTCStats(0, "remote-candidate", "remote", mapOf()),
        "pair" to RTCStats(0, "candidate-pair", "pair", mapOf()),
        "transport" to RTCStats(0, "transport", "transport", mapOf("selectedCandidatePairId" to "pair")))
    interceptor.process(connection, OnIceConnectionChange(IceConnectionState.CONNECTED), "local1", "remote1", "con1", stats)
  }
}