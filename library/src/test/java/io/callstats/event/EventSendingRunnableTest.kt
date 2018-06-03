package io.callstats.event

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
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

    val event = TestEvent(server.url("").toString())
    val request = event.toRequest(gson)
    val runnable = EventSendingRunnable(client, request)

    var isSuccess = false
    var responseString: String? = null
    runnable.callback = { success, body ->
      isSuccess = success
      responseString = body
    }
    runnable.run()

    assertTrue(isSuccess)
    assertEquals("{}", responseString)
  }

  @Test
  fun failResponse() {
    val response = MockResponse().setBody("{}").setResponseCode(500)
    server.enqueue(response)

    val event = TestEvent(server.url("").toString())
    val request = event.toRequest(gson)
    val runnable = EventSendingRunnable(client, request)

    var isSuccess = false
    var responseString: String? = null
    runnable.callback = { success, body ->
      isSuccess = success
      responseString = body
    }
    runnable.run()

    assertFalse(isSuccess)
    assertEquals("{}", responseString)
  }

  @After
  fun tearDown() {
    server.shutdown()
  }

  class TestEvent(private val testUrl: String): Event() {
    override fun url(): String = testUrl
    override fun path(): String = ""
  }
}