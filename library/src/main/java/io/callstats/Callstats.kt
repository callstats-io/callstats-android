package io.callstats

import android.content.Context
import io.callstats.event.EventSender
import io.callstats.event.auth.TokenRequest
import io.callstats.event.fabric.FabricSetupFailedEvent
import io.callstats.event.user.UserAliveEvent
import io.callstats.event.user.UserJoinEvent
import io.callstats.event.user.UserLeftEvent
import io.callstats.event.EventManager
import io.callstats.event.EventManagerImpl
import io.callstats.event.EventSenderImpl
import io.callstats.event.info.Feedback
import io.callstats.event.special.FeedbackEvent
import io.callstats.event.special.LogEvent
import io.callstats.event.stats.SystemStatusStats
import io.callstats.utils.SystemStatus
import io.callstats.utils.SystemStatusProvider
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.webrtc.PeerConnection
import java.util.Timer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.timerTask

/**
 * Entry point for sending WebRTC stats to callstats.io
 */
class Callstats(
    private val context: Context,
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
        config: CallstatsConfig): EventManager = EventManagerImpl(sender, remoteID, connection, config)
    open fun eventSender(
        client: OkHttpClient,
        executor: ExecutorService,
        appID: String,
        localID: String,
        deviceID: String): EventSender = EventSenderImpl(client, executor, appID, localID, deviceID)
    open fun systemStatus(): SystemStatusProvider = SystemStatus()
  }

  companion object {
    internal var dependency = Dependency()
  }

  private val okHttpClient = dependency.okhttpClient()
  private val executor = dependency.executor()
  private val sender = dependency.eventSender(okHttpClient, executor, appID, localID, deviceID)
  private val systemStatus = dependency.systemStatus()

  // timers
  private var aliveTimer: Timer? = null
  private var systemStatsTimer: Timer? = null

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
    startSendingSystemStats()
  }

  /**
   * Stop the current session and stop the keep alive.
   */
  fun stopSession() {
    stopSendingSystemStats()
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

  /**
   * Log application event
   * @param message message to be logged
   * @param level level of this log message
   * @param type type of message content
   */
  fun log(message: String, level: LoggingLevel = LoggingLevel.INFO, type: LoggingType = LoggingType.TEXT) {
    sender.send(LogEvent(level.name.toLowerCase(), message, type.name.toLowerCase()))
  }

  /**
   * Give feedback on this conference call
   */
  fun feedback(
      rating: Int,
      comment: String? = null,
      audioQuality: Int? = null,
      videoQuality: Int? = null,
      remoteUserID: String? = null)
  {
    val info = Feedback(rating).apply {
      comments = comment
      audioQualityRating = audioQuality
      videoQualityRating = videoQuality
      remoteID = remoteUserID
    }
    sender.send(FeedbackEvent(info))
  }

  // region Timers

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

  private fun startSendingSystemStats() {
    stopSendingSystemStats()
    val period = configuration.systemStatsSubmissionPeriod * 1000L
    systemStatsTimer = Timer(true)
    systemStatsTimer?.schedule(
        timerTask {
          val stats = SystemStatusStats().apply {
            cpuLevel = systemStatus.cpuLevel()
            batteryLevel = systemStatus.batteryLevel(context)
            memoryAvailable = systemStatus.availableMemory(context)
            memoryUsage = systemStatus.usageMemory(context)
            threadCount = systemStatus.threadCount()
          }
          if (stats.isValid()) sender.send(stats)
        },
        period,
        period)
  }

  private fun stopSendingSystemStats() {
    systemStatsTimer?.cancel()
    systemStatsTimer = null
  }

  // endregion
}