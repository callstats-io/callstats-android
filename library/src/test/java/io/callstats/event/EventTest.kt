package io.callstats.event

import com.google.gson.Gson
import io.callstats.event.auth.TokenRequest
import io.callstats.event.fabric.FabricTerminatedEvent
import io.callstats.event.user.UserJoinEvent
import okhttp3.RequestBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventTest {

  private val gson = Gson()

  @Test
  fun convertTokenRequestToRequest() {
    val tokenRequest = TokenRequest("sample_code", "client123")
    val request = tokenRequest.toRequest(gson)

    assertEquals(
        "grant_type=authorization_code&client_id=client123&code=sample_code",
        requestBodyToString(request.body()))
  }

  @Test(expected = IllegalStateException::class)
  fun convertInvalidAuthenticatedEventToRequest() {
    val event = UserJoinEvent("conf1")
    event.toRequest(gson)
    event.appID = "app1"
    event.toRequest(gson)
    event.token = "token"
    event.toRequest(gson)
  }

  @Test
  fun convertValidAuthenticatedEventToRequest() {
    val event = UserJoinEvent("conf1")
    event.appID = "app1"
    event.token = "1234"

    val request = event.toRequest(gson)
    assertEquals("Bearer 1234", request.header("Authorization"))

    val bodyString = requestBodyToString(request.body())
    val json = gson.fromJson(bodyString, UserJoinEvent::class.java)
    assertNull(json.token)
  }

  @Test(expected = IllegalStateException::class)
  fun convertInvalidSessionEventToRequest() {
    val event = FabricTerminatedEvent("remote1", "con1")
    event.toRequest(gson)
    event.appID = "app1"
    event.toRequest(gson)
    event.confID = "conf1"
    event.toRequest(gson)
    event.token = "token"
    event.toRequest(gson)
  }

  @Test
  fun convertValidSessionEventToRequest() {
    val event = FabricTerminatedEvent("remote1", "con1")
    event.appID = "app1"
    event.confID = "conf1"
    event.token = "1234"
    event.ucID = "uc1"

    val request = event.toRequest(gson)
    assertEquals("Bearer 1234", request.header("Authorization"))

    val bodyString = requestBodyToString(request.body())
    val json = gson.fromJson(bodyString, FabricTerminatedEvent::class.java)
    assertNull(json.token)
    assertEquals(json.remoteID, "remote1")
    assertEquals(json.connectionID, "con1")
  }

  // utils

  private fun requestBodyToString(body: RequestBody?): String {
    val buffer = Buffer()
    body?.writeTo(buffer)
    return buffer.readUtf8()
  }
}