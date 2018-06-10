package io.callstats

import io.callstats.event.EventSender
import io.callstats.event.auth.TokenRequest
import io.callstats.event.user.UserAliveEvent
import io.callstats.event.user.UserJoinEvent
import io.callstats.event.user.UserLeftEvent
import io.callstats.interceptor.InterceptorManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.timerTask

/**
 * Entry point for sending WebRTC stats to callstats.io
 */
class Callstats(
    appID: String,
    localID: String,
    deviceID: String,
    jwt: String,
    private val clientVersion: String? = null,
    private val configuration: CallstatsConfig = CallstatsConfig()) {

  internal open class Dependency {
    open fun okhttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()
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

  // timers
  private var aliveTimer: Timer? = null

  init {
    sender.send(TokenRequest(jwt, "$localID@$appID"))
  }

  /**
   * Start the user session when creating conference call.
   * This will start sending keep alive as well.
   * @param confID local conference identifier for this call session
   */
  fun startSession(confID: String) {
    sender.send(UserJoinEvent(confID, clientVersion).apply { this.confID = confID })
    startKeepAlive()
  }

  /**
   * Stop the current session and stop the keep alive.
   */
  fun stopSession() {
    stopKeepAlive()
    sender.send(UserLeftEvent())
  }

  // timers

  private fun startKeepAlive() {
    stopKeepAlive()
    val period = configuration.keepAlivePeriod * 1000L
    aliveTimer = Timer(true)
    aliveTimer?.schedule(
        timerTask { sender.send(UserAliveEvent()) },
        period,
        period)
  }

  private fun stopKeepAlive() {
    aliveTimer?.cancel()
    aliveTimer = null
  }
}