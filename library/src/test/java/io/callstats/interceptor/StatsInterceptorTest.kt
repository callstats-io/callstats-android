package io.callstats.interceptor

import com.nhaarman.mockito_kotlin.whenever
import io.callstats.OnIceConnectionChange
import io.callstats.OnStats
import io.callstats.event.Event
import io.callstats.event.stats.ConferenceStats
import io.callstats.utils.WifiStatusProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.webrtc.PeerConnection
import org.webrtc.RTCStats

class StatsInterceptorTest {

  @Mock private lateinit var connection: PeerConnection
  @Mock private lateinit var wifiStatusProvider: WifiStatusProvider

  private lateinit var interceptor: StatsInterceptor

  private val inboundStats = mapOf(
      "id2" to RTCStats(0, "inbound-rtp", "id2", mapOf(
          "bytesReceived" to 1,
          "jitter" to 1.0,
          "packetsReceived" to 1,
          "packetsLost" to 1
      )))
  private val outboundStats = mapOf(
      "id1" to RTCStats(0, "outbound-rtp", "id1", mapOf(
          "bytesSent" to 1,
          "roundTripTime" to 1.0
      )))

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
    interceptor = StatsInterceptor(wifiStatusProvider)

    // connected
    interceptor.process(
        connection,
        OnIceConnectionChange(PeerConnection.IceConnectionState.CONNECTED),
        "local1",
        "remote1",
        "connection1",
        emptyMap())
  }

  @Test
  fun conferenceStats() {
    val events = process(emptyMap())
    assertEquals(1, events.size)
    assertTrue(events.first() is ConferenceStats)
  }

  @Test
  fun sendWifiStats() {
    whenever(wifiStatusProvider.wifiRssi()).thenReturn(-1)
    whenever(wifiStatusProvider.wifiSignal()).thenReturn(20)
    val events = process(emptyMap())
    val event = events.first() as ConferenceStats
    assertEquals(-1, event.wifiStats?.rssi)
    assertEquals(20, event.wifiStats?.signal)
  }

  @Test
  fun sendCsioAvgBRKbps() {
    val outEvents = process(outboundStats)
    val outEvent = outEvents.first() as ConferenceStats
    assertTrue(outEvent.stats.any { (it as Map<*, *>).containsKey("csioAvgBRKbps") })
    val inEvents = process(inboundStats)
    val inEvent = inEvents.first() as ConferenceStats
    assertTrue(inEvent.stats.any { (it as Map<*, *>).containsKey("csioAvgBRKbps") })
  }

  @Test
  fun sendCsioIntBRKbps() {
    // process first event then csioIntBRKbps can use the different timestamp
    process(emptyMap())
    val outEvents = process(outboundStats)
    val outEvent = outEvents.first() as ConferenceStats
    assertTrue(outEvent.stats.any { (it as Map<*, *>).containsKey("csioIntBRKbps") })
    val inEvents = process(inboundStats)
    val inEvent = inEvents.first() as ConferenceStats
    assertTrue(inEvent.stats.any { (it as Map<*, *>).containsKey("csioIntBRKbps") })
  }

  @Test
  fun outboundSendCsioAvgRtt() {
    val events = process(outboundStats)
    val event = events.first() as ConferenceStats
    assertTrue(event.stats.any { (it as Map<*, *>).containsKey("csioAvgRtt") })
  }

  @Test
  fun outboundSendCsioIntMs() {
    // process first event then csioIntBRKbps can use the different timestamp
    process(emptyMap())
    val events = process(outboundStats)
    val event = events.first() as ConferenceStats
    assertTrue(event.stats.any { (it as Map<*, *>).containsKey("csioIntMs") })
  }

  @Test
  fun outboundSendCsioTimeElapseMs() {
    val events = process(outboundStats)
    val event = events.first() as ConferenceStats
    assertTrue(event.stats.any { (it as Map<*, *>).containsKey("csioTimeElapseMs") })
  }

  @Test
  fun inboundSendCsioAvgJitter() {
    val events = process(inboundStats)
    val event = events.first() as ConferenceStats
    assertTrue(event.stats.any { (it as Map<*, *>).containsKey("csioAvgJitter") })
  }

  @Test
  fun inboundSendCsioIntFL() {
    val events = process(inboundStats)
    val event = events.first() as ConferenceStats
    assertTrue(event.stats.any { (it as Map<*, *>).containsKey("csioIntFL") })
  }

  @Test
  fun inboundSendCsioIntPktLoss() {
    val events = process(inboundStats)
    val event = events.first() as ConferenceStats
    assertTrue(event.stats.any { (it as Map<*, *>).containsKey("csioIntPktLoss") })
  }

  // utils

  private fun process(stats: Map<String, RTCStats>): Array<Event> {
    Thread.sleep(10) // add some time between event processing
    return interceptor.process(
        connection,
        OnStats(),
        "local1",
        "remote1",
        "connection1",
        stats)
  }
}