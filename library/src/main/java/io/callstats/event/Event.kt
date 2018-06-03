package io.callstats.event

import com.google.gson.Gson
import io.callstats.event.auth.TokenRequest
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody

/**
 * Base event
 */
abstract class Event {

  companion object {
    private const val HEADER_AUTHORIZATION = "Authorization"
    private val MEDIA_JSON = MediaType.parse("application/json; charset=utf-8")
    private val MEDIA_URLENCODED = MediaType.parse("application/x-www-form-urlencoded")
  }

  internal var localID: String = ""
  internal var deviceID: String = ""
  internal var timestamp: Long = 0L
  internal var originID: String? = null

  open fun url(): String {
    return "https://events.callstats.io"
  }

  abstract fun path(): String

  /**
   * Convert event to okHttp request
   */
  fun toRequest(gson: Gson): Request {
    // check
    if (this is AuthenticatedEvent) {
      checkNotNull(appID)
      checkNotNull(confID)
      checkNotNull(token)
    }
    if (this is SessionEvent) {
      checkNotNull(ucID)
    }

    // url
    val path = when (this) {
      is SessionEvent -> "v1/apps/$appID/conferences/$confID/$ucID/events/${path()}"
      is AuthenticatedEvent -> "v1/apps/$appID/conferences/$confID"
      else -> path()
    }
    val url = "${url()}/$path"

    // content
    val content = if (this is TokenRequest) {
      "grant_type=$grantType&client_id=$clientID&code=$code"
    } else {
      gson.toJson(this)
    }
    val media = if (this is TokenRequest) MEDIA_URLENCODED else MEDIA_JSON
    val body = RequestBody.create(media, content)

    val request = Request.Builder()
        .url(url)
        .post(body)

    // add auth token to header if needed by event
    if (this is AuthenticatedEvent) {
      request.header(HEADER_AUTHORIZATION, "Bearer $token")
    }
    return request.build()
  }
}

abstract class AuthenticatedEvent : Event() {
  @Transient var appID: String? = null
  @Transient var confID: String? = null
  @Transient var token: String? = null
}

abstract class SessionEvent: AuthenticatedEvent() {
  @Transient var ucID: String? = null
}