package io.callstats

import io.callstats.event.EventSender
import io.callstats.event.auth.TokenRequest
import io.callstats.event.fabric.FabricSetupFailedEvent
import io.callstats.event.user.UserAliveEvent
import io.callstats.event.user.UserJoinEvent
import io.callstats.event.user.UserLeftEvent
import io.callstats.event.EventManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.webrtc.PeerConnection
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
    open fun eventManager(
        sender: EventSender,
        remoteID: String,
        connection: PeerConnection,
        config: CallstatsConfig): EventManager = EventManager(sender, remoteID, connection, config)
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
  private var sender = dependency.eventSender(okHttpClient, executor, appID, localID, deviceID)

  // timers
  private var aliveTimer: Timer? = null

  // connections
  internal val eventManagers = mutableMapOf<String, EventManager>()

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

  // events

  /**
   * Create new connection. Call this before [reportEvent]
   * @param connection reporting PeerConnection object
   * @param remoteUserID recipient's userID
   */
  fun addNewFabric(connection: PeerConnection, remoteUserID: String) {
    if (eventManagers.containsKey(remoteUserID)) return
    eventManagers[remoteUserID] = dependency.eventManager(sender, remoteUserID, connection, configuration)
  }

  /**
   * Report normal WebRTC event from observer
   * @param remoteUserID recipient's userID
   */
  fun reportEvent(remoteUserID: String, type: CallstatsWebRTCFunction) {
    eventManagers[remoteUserID]?.process(type)
  }

  /**
   * Report error
   * @param type [CallstatsError]
   */
  fun reportError(type: CallstatsError, message: String? = null, stack: String? = null) {
    sender.send(FabricSetupFailedEvent(type.value).apply {
      this.message = message
      this.stack = stack
    })
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