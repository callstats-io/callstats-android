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
import org.webrtc.PeerConnection.IceConnectionState
import org.webrtc.RTCStats

class IceInterceptorTest {

  private lateinit var interceptor: IceInterceptor
  private lateinit var stats: Map<String, RTCStats>

  @Before
  fun setup() {
    stats = mapOf(
        "local" to RTCStats(0, "local-candidate", "id1", mapOf()),
        "remote" to RTCStats(0, "remote-candidate", "id2", mapOf()),
        "pair" to RTCStats(0, "candidate-pair", "id3", mapOf()))
    interceptor = IceInterceptor("remote1")
  }

  @Test
  fun iceDisruptionStart() {
    interceptor.process(OnIceConnectionChange(IceConnectionState.CONNECTED), "con1", stats)
    val events = interceptor.process(OnIceConnectionChange(IceConnectionState.DISCONNECTED), "con1", stats)
    assertTrue(events.any { it is IceDisruptStartEvent })

    interceptor.process(OnIceConnectionChange(IceConnectionState.COMPLETED), "con1", stats)
    val events2 = interceptor.process(OnIceConnectionChange(IceConnectionState.DISCONNECTED), "con1", stats)
    assertTrue(events2.any { it is IceDisruptStartEvent })
  }

  @Test
  fun iceDisruptionEnd() {
    interceptor.process(OnIceConnectionChange(IceConnectionState.DISCONNECTED), "con1", stats)
    val events = interceptor.process(OnIceConnectionChange(IceConnectionState.CONNECTED), "con1", stats)
    assertTrue(events.any { it is IceDisruptEndEvent })

    interceptor.process(OnIceConnectionChange(IceConnectionState.DISCONNECTED), "con1", stats)
    val events2 = interceptor.process(OnIceConnectionChange(IceConnectionState.COMPLETED), "con1", stats)
    assertTrue(events2.any { it is IceDisruptEndEvent })

    interceptor.process(OnIceConnectionChange(IceConnectionState.DISCONNECTED), "con1", stats)
    val events3 = interceptor.process(OnIceConnectionChange(IceConnectionState.CHECKING), "con1", stats)
    assertTrue(events3.any { it is IceDisruptEndEvent })
  }

  @Test
  fun iceRestart() {
    interceptor.process(OnIceConnectionChange(IceConnectionState.COMPLETED), "con1", stats)
    val events = interceptor.process(OnIceConnectionChange(IceConnectionState.NEW), "con1", stats)
    assertTrue(events.any { it is IceRestartEvent })
  }

  @Test
  fun iceFailed() {
    interceptor.process(OnIceConnectionChange(IceConnectionState.CHECKING), "con1", stats)
    val events = interceptor.process(OnIceConnectionChange(IceConnectionState.FAILED), "con1", stats)
    assertTrue(events.any { it is IceFailedEvent })

    interceptor.process(OnIceConnectionChange(IceConnectionState.DISCONNECTED), "con1", stats)
    val events2 = interceptor.process(OnIceConnectionChange(IceConnectionState.FAILED), "con1", stats)
    assertTrue(events2.any { it is IceFailedEvent })
  }

  @Test
  fun iceAbort() {
    interceptor.process(OnIceConnectionChange(IceConnectionState.CHECKING), "con1", stats)
    val events = interceptor.process(OnIceConnectionChange(IceConnectionState.CLOSED), "con1", stats)
    assertTrue(events.any { it is IceAbortedEvent })

    interceptor.process(OnIceConnectionChange(IceConnectionState.NEW), "con1", stats)
    val events2 = interceptor.process(OnIceConnectionChange(IceConnectionState.CLOSED), "con1", stats)
    assertTrue(events2.any { it is IceAbortedEvent })
  }

  @Test
  fun iceTerminated() {
    interceptor.process(OnIceConnectionChange(IceConnectionState.CONNECTED), "con1", stats)
    val events = interceptor.process(OnIceConnectionChange(IceConnectionState.CLOSED), "con1", stats)
    assertTrue(events.any { it is IceTerminatedEvent })

    interceptor.process(OnIceConnectionChange(IceConnectionState.COMPLETED), "con1", stats)
    val events2 = interceptor.process(OnIceConnectionChange(IceConnectionState.CLOSED), "con1", stats)
    assertTrue(events2.any { it is IceTerminatedEvent })

    interceptor.process(OnIceConnectionChange(IceConnectionState.FAILED), "con1", stats)
    val events3 = interceptor.process(OnIceConnectionChange(IceConnectionState.CLOSED), "con1", stats)
    assertTrue(events3.any { it is IceTerminatedEvent })

    interceptor.process(OnIceConnectionChange(IceConnectionState.DISCONNECTED), "con1", stats)
    val events4 = interceptor.process(OnIceConnectionChange(IceConnectionState.CLOSED), "con1", stats)
    assertTrue(events4.any { it is IceTerminatedEvent })
  }

  @Test
  fun iceConnectionDisruptionStart() {
    interceptor.process(OnIceConnectionChange(IceConnectionState.CHECKING), "con1", stats)
    val events = interceptor.process(OnIceConnectionChange(IceConnectionState.DISCONNECTED), "con1", stats)
    assertTrue(events.any { it is IceConnectionDisruptStartEvent })
  }

  @Test
  fun iceConnectionDisruptionEnd() {
    interceptor.process(OnIceConnectionChange(IceConnectionState.DISCONNECTED), "con1", stats)
    val events = interceptor.process(OnIceConnectionChange(IceConnectionState.CHECKING), "con1", stats)
    assertTrue(events.any { it is IceConnectionDisruptEndEvent })
  }
}