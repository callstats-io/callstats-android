package io.callstats.event

import com.google.gson.Gson
import io.callstats.event.auth.TokenRequest
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody

/**
 * Base event
 */
internal abstract class Event {

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

  open fun path(): String {
    return ""
  }

  /**
   * Convert event to okHttp request
   */
  fun toRequest(gson: Gson): Request {
    // check
    if (this is AuthenticatedEvent) {
      checkNotNull(appID)
      checkNotNull(token)
    }
    if (this is SessionEvent) {
      checkNotNull(ucID)
      checkNotNull(confID)
    }

    // url
    val path = when (this) {
      is SessionEvent -> "v1/apps/$appID/conferences/$confID/$ucID/${path()}"
      is CreateSessionEvent -> "v1/apps/$appID/conferences/$confID"
      is AuthenticatedEvent -> "v1/apps/$appID/${path()}"
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

/**
 * Event that can be sent after authenticated
 */
internal abstract class AuthenticatedEvent : Event() {
  @Transient var appID: String? = null
  @Transient var token: String? = null
}

/**
 * Event that can be sent after session created
 */
internal abstract class SessionEvent: AuthenticatedEvent() {
  @Transient var ucID: String? = null
  @Transient var confID: String? = null
}

/**
 * Event to keep the session alive
 */
internal abstract class KeepAliveEvent: SessionEvent()

/**
 * Event to create session
 */
internal abstract class CreateSessionEvent: AuthenticatedEvent() {
  @Transient var confID: String? = null
}

/**
 * Event that do authentication
 */
internal interface AuthenticationEvent {
  val code: String
  val clientID: String
}