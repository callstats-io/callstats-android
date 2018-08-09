package io.callstats.interceptor

import io.callstats.OnIceConnectionChange
import io.callstats.event.ice.IceAbortedEvent
import io.callstats.event.ice.IceConnectionDisruptEndEvent
import io.callstats.event.ice.IceConnectionDisruptStartEvent
import io.callstats.event.ice.IceDisruptEndEvent
import io.callstats.event.ice.IceDisruptStartEvent
import io.callstats.event.ice.IceFailedEvent
import io.callstats.event.ice.IceRestartEvent
import io.callstats.event.ice.IceTerminatedEvent
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceConnectionState
import org.webrtc.RTCStats

class IceInterceptorTest {
  
  @Mock private lateinit var connection: PeerConnection

  private lateinit var interceptor: IceInterceptor
  private lateinit var stats: Map<String, RTCStats>

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
    stats = mapOf(
        "local" to RTCStats(0, "local-candidate", "id1", mapOf()),
        "remote" to RTCStats(0, "remote-candidate", "id2", mapOf()),
        "pair" to RTCStats(0, "candidate-pair", "id3", mapOf()))
    interceptor = IceInterceptor()
  }

  @Test
  fun iceDisruptionStart() {
    interceptor.process(connection, OnIceConnectionChange(IceConnectionState.CONNECTED), "local1", "remote1", "con1", stats)
    val events = interceptor.process(connection, OnIceConnectionChange(IceConnectionState.DISCONNECTED), "local1", "remote1", "con1", stats)
    assertTrue(events.any { it is IceDisruptStartEvent })

    interceptor.process(connection, OnIceConnectionChange(IceConnectionState.COMPLETED), "local1", "remote1", "con1", stats)
    val events2 = interceptor.process(connection, OnIceConnectionChange(IceConnectionState.DISCONNECTED), "local1", "remote1", "con1", stats)
    assertTrue(events2.any { it is IceDisruptStartEvent })
  }

  @Test
  fun iceDisruptionEnd() {
    interceptor.process(connection, OnIceConnectionChange(IceConnectionState.DISCONNECTED), "local1", "remote1", "con1", stats)
    val events = interceptor.process(connection, OnIceConnectionChange(IceConnectionState.CONNECTED), "local1", "remote1", "con1", stats)
    assertTrue(events.any { it is IceDisruptEndEvent })

    interceptor.process(connection, OnIceConnectionChange(IceConnectionState.DISCONNECTED), "local1", "remote1", "con1", stats)
    val events2 = interceptor.process(connection, OnIceConnectionChange(IceConnectionState.COMPLETED), "local1", "remote1", "con1", stats)
    assertTrue(events2.any { it is IceDisruptEndEvent })

    interceptor.process(connection, OnIceConnectionChange(IceConnectionState.DISCONNECTED), "local1", "remote1", "con1", stats)
    val events3 = interceptor.process(connection, OnIceConnectionChange(IceConnectionState.CHECKING), "local1", "remote1", "con1", stats)
    assertTrue(events3.any { it is IceDisruptEndEvent })
  }

  @Test
  fun iceRestart() {
    interceptor.process(connection, OnIceConnectionChange(IceConnectionState.COMPLETED), "local1", "remote1", "con1", stats)
    val events = interceptor.process(connection, OnIceConnectionChange(IceConnectionState.NEW), "local1", "remote1", "con1", stats)
    assertTrue(events.any { it is IceRestartEvent })
  }

  @Test
  fun iceFailed() {
    interceptor.process(connection, OnIceConnectionChange(IceConnectionState.CHECKING), "local1", "remote1", "con1", stats)
    val events = interceptor.process(connection, OnIceConnectionChange(IceConnectionState.FAILED), "local1", "remote1", "con1", stats)
    assertTrue(events.any { it is IceFailedEvent })

    interceptor.process(connection, OnIceConnectionChange(IceConnectionState.DISCONNECTED), "local1", "remote1", "con1", stats)
    val events2 = interceptor.process(connection, OnIceConnectionChange(IceConnectionState.FAILED), "local1", "remote1", "con1", stats)
    assertTrue(events2.any { it is IceFailedEvent })
  }

  @Test
  fun iceAbort() {
    interceptor.process(connection, OnIceConnectionChange(IceConnectionState.CHECKING), "local1", "remote1", "con1", stats)
    val events = interceptor.process(connection, OnIceConnectionChange(IceConnectionState.CLOSED), "local1", "remote1", "con1", stats)
    assertTrue(events.any { it is IceAbortedEvent })

    interceptor.process(connection, OnIceConnectionChange(IceConnectionState.NEW), "local1", "remote1", "con1", stats)
    val events2 = interceptor.process(connection, OnIceConnectionChange(IceConnectionState.CLOSED), "local1", "remote1", "con1", stats)
    assertTrue(events2.any { it is IceAbortedEvent })
  }

  @Test
  fun iceTerminated() {
    interceptor.process(connection, OnIceConnectionChange(IceConnectionState.CONNECTED), "local1", "remote1", "con1", stats)
    val events = interceptor.process(connection, OnIceConnectionChange(IceConnectionState.CLOSED), "local1", "remote1", "con1", stats)
    assertTrue(events.any { it is IceTerminatedEvent })

    interceptor.process(connection, OnIceConnectionChange(IceConnectionState.COMPLETED), "local1", "remote1", "con1", stats)
    val events2 = interceptor.process(connection, OnIceConnectionChange(IceConnectionState.CLOSED), "local1", "remote1", "con1", stats)
    assertTrue(events2.any { it is IceTerminatedEvent })

    interceptor.process(connection, OnIceConnectionChange(IceConnectionState.FAILED), "local1", "remote1", "con1", stats)
    val events3 = interceptor.process(connection, OnIceConnectionChange(IceConnectionState.CLOSED), "local1", "remote1", "con1", stats)
    assertTrue(events3.any { it is IceTerminatedEvent })

    interceptor.process(connection, OnIceConnectionChange(IceConnectionState.DISCONNECTED), "local1", "remote1", "con1", stats)
    val events4 = interceptor.process(connection, OnIceConnectionChange(IceConnectionState.CLOSED), "local1", "remote1", "con1", stats)
    assertTrue(events4.any { it is IceTerminatedEvent })
  }

  @Test
  fun iceConnectionDisruptionStart() {
    interceptor.process(connection, OnIceConnectionChange(IceConnectionState.CHECKING), "local1", "remote1", "con1", stats)
    val events = interceptor.process(connection, OnIceConnectionChange(IceConnectionState.DISCONNECTED), "local1", "remote1", "con1", stats)
    assertTrue(events.any { it is IceConnectionDisruptStartEvent })
  }

  @Test
  fun iceConnectionDisruptionEnd() {
    interceptor.process(connection, OnIceConnectionChange(IceConnectionState.DISCONNECTED), "local1", "remote1", "con1", stats)
    val events = interceptor.process(connection, OnIceConnectionChange(IceConnectionState.CHECKING), "local1", "remote1", "con1", stats)
    assertTrue(events.any { it is IceConnectionDisruptEndEvent })
  }
}