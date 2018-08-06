package io.callstats.interceptor

import io.callstats.OnStats
import io.callstats.event.stats.ConferenceStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StatsInterceptorTest {

  private lateinit var interceptor: StatsInterceptor

  @Before
  fun setup() {
    interceptor = StatsInterceptor()
  }

  @Test
  fun conferenceStats() {
    val events = interceptor.process(OnStats(), "remote1", "connection1", mapOf())
    assertEquals(1, events.size)
    assertTrue(events.first() is ConferenceStats)
  }
}