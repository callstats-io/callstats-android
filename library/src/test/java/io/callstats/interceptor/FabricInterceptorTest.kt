package io.callstats.interceptor

import io.callstats.OnIceConnectionChange
import io.callstats.event.fabric.FabricSetupEvent
import io.callstats.event.fabric.FabricStateChangeEvent
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.webrtc.PeerConnection.IceConnectionState
import org.webrtc.RTCStats

class FabricInterceptorTest {

  private lateinit var interceptor: FabricInterceptor

  @Before
  fun setup() {
    interceptor = FabricInterceptor("remote1")
  }

  @Test
  fun fabricConnectSend() {
    val stats = mapOf<String, RTCStats>()
    val events = interceptor.process(OnIceConnectionChange(IceConnectionState.CONNECTED), "con1", stats)
    assertEquals(1, events.size)
    assertTrue(events.first() is FabricSetupEvent)
  }

  @Test
  fun fabricConnectOnlyFirstTime() {
    connected()
    val stats = mapOf<String, RTCStats>()
    val events = interceptor.process(OnIceConnectionChange(IceConnectionState.CONNECTED), "con1", stats)
    assertEquals(0, events.size)
    interceptor.process(OnIceConnectionChange(IceConnectionState.FAILED), "con1", stats)
    val events2 = interceptor.process(OnIceConnectionChange(IceConnectionState.CONNECTED), "con1", stats)
    assertEquals(1, events2.size)
    assertFalse(events2.first() is FabricSetupEvent)
  }

  @Test
  fun fabricStateChangeShouldNotSendIfNotConnected() {
    val stats = mapOf<String, RTCStats>()
    val events = interceptor.process(OnIceConnectionChange(IceConnectionState.CHECKING), "con1", stats)
    assertEquals(0, events.size)
  }

  @Test
  fun fabricStateSend() {
    connected()
    val stats = mapOf<String, RTCStats>()
    val events = interceptor.process(OnIceConnectionChange(IceConnectionState.CHECKING), "con1", stats)
    assertEquals(1, events.size)
    assertTrue(events.first() is FabricStateChangeEvent)
  }

  // utils

  private fun connected() {
    interceptor.process(OnIceConnectionChange(IceConnectionState.CONNECTED), "con1", mapOf())
  }
}