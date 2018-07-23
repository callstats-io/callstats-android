package io.callstats.event

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EventSendingRunnableTest {

  private val client = OkHttpClient()
  private val gson = Gson()
  lateinit var server: MockWebServer

  @Before
  fun setup() {
    server = MockWebServer()
  }

  @Test
  fun successfulResponse() {
    val response = MockResponse().setBody("{}")
    server.enqueue(response)

    val event = object : Event() {
      override fun url(): String = server.url("").toString()
    }
    val runnable = EventSendingRunnable(client, event, gson)

    var isSuccess = false
    var responseMap: Map<String, Any?>? = null
    runnable.callback = { _, success, map ->
      isSuccess = success
      responseMap = map
    }
    runnable.run()

    assertTrue(isSuccess)
    assertEquals(0, responseMap?.size)
  }

  @Test
  fun failResponse() {
    val response = MockResponse().setBody("{'error': 'error'}").setResponseCode(500)
    server.enqueue(response)

    val event = object : Event() {
      override fun url(): String = server.url("").toString()
    }
    val runnable = EventSendingRunnable(client, event, gson)

    var isSuccess = false
    var responseMap: Map<String, Any?>? = null
    runnable.callback = { _, success, map ->
      isSuccess = success
      responseMap = map
    }
    runnable.run()

    assertFalse(isSuccess)
    assertEquals(1, responseMap?.size)
  }

  @After
  fun tearDown() {
    server.shutdown()
  }
}