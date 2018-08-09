package io.callstats.interceptor

import io.callstats.OnStats
import io.callstats.event.stats.ConferenceStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.webrtc.PeerConnection

class StatsInterceptorTest {

  @Mock private lateinit var connection: PeerConnection

  private lateinit var interceptor: StatsInterceptor

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
    interceptor = StatsInterceptor()
  }

  @Test
  fun conferenceStats() {
    val events = interceptor.process(connection, OnStats(), "local1", "remote1", "connection1", mapOf())
    assertEquals(1, events.size)
    assertTrue(events.first() is ConferenceStats)
  }
}