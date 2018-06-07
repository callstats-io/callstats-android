package io.callstats

import io.callstats.event.EventSender
import io.callstats.event.auth.TokenRequest
import io.callstats.event.user.UserJoinEvent
import io.callstats.event.user.UserLeftEvent
import io.callstats.interceptor.InterceptorManager
import okhttp3.OkHttpClient
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Entry point for sending WebRTC stats to callstats.io
 */
class Callstats(appID: String, localID: String, deviceID: String, jwt: String) {

  internal open class Dependency {
    open fun okhttpClient(): OkHttpClient = OkHttpClient()
    open fun executor(): ExecutorService = Executors.newSingleThreadExecutor()
    open fun interceptorManager(): InterceptorManager = InterceptorManager(emptyArray())
    open fun eventSender(
        client: OkHttpClient,
        executor: ExecutorService,
        appID: String,
        localID: String,
        deviceID: String): EventSender = EventSender(client, executor, appID, localID, deviceID)
  }

  companion object {
    internal var dependency = Dependency()
  }

  private val okHttpClient = dependency.okhttpClient()
  private val executor = dependency.executor()
  private val interceptorManager = dependency.interceptorManager()
  private var sender = dependency.eventSender(okHttpClient, executor, appID, localID, deviceID)

  init {
    sender.send(TokenRequest(jwt, "$localID@$appID"))
  }

  /**
   * Start the user session when creating conference call.
   * This will start sending keep alive as well.
   * @param confID local conference identifier for this call session
   */
  fun startSession(confID: String) {
    sender.send(UserJoinEvent().apply { this.confID = confID })
  }

  /**
   * Stop the current session and stop the keep alive.
   */
  fun stopSession() {
    sender.send(UserLeftEvent())
  }
}