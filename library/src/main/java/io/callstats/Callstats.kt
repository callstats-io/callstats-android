package io.callstats

import android.content.Context
import io.callstats.event.auth.TokenRequest
import io.callstats.event.fabric.FabricSetupFailedEvent
import io.callstats.event.user.UserAliveEvent
import io.callstats.event.user.UserJoinEvent
import io.callstats.event.user.UserLeftEvent
import io.callstats.event.EventManager
import io.callstats.event.device.DeviceEvent
import io.callstats.event.info.Feedback
import io.callstats.event.special.DominantSpeakerEvent
import io.callstats.event.special.FeedbackEvent
import io.callstats.event.special.LogEvent
import io.callstats.event.stats.SystemStatusStats
import io.callstats.event.user.UserDetailsEvent
import org.webrtc.PeerConnection
import java.util.Timer
import kotlin.concurrent.timerTask

/**
 * Entry point for sending WebRTC stats to callstats.io
 *
 * @param context Application context
 * @param appID App identifier from Callstats
 * @param localID Your user identifier for this conference
 * @param deviceID Unique device identifier
 * @param jwt JWT from server
 * @param username Your readable username to show in dashboard
 * @param clientVersion Your app version
 * @param configuration [CallstatsConfig] lib config
 */
class Callstats(
    private val context: Context,
    appID: String,
    private val localID: String,
    deviceID: String,
    jwt: String,
    private val username: String? = null,
    private val clientVersion: String? = null,
    private val configuration: CallstatsConfig = CallstatsConfig()) {

  companion object {
    internal var dependency = CallstatsInjector()
  }

  private val sender = dependency.eventSender(appID, localID, deviceID)
  private val systemStatus = dependency.systemStatus()

  // timers
  private var aliveTimer: Timer? = null
  private var systemStatsTimer: Timer? = null

  // event manager for each connection
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
    sender.send(UserJoinEvent(confID, clientVersion))
    username?.let { sender.send(UserDetailsEvent(it)) }
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
    eventManagers[remoteUserID] = dependency.eventManager(
        context,
        sender,
        localID,
        remoteUserID,
        connection,
        configuration)
  }

  /**
   * Report normal WebRTC event from observer
   * @param remoteUserID recipient's userID
   */
  fun reportEvent(remoteUserID: String, type: PeerEvent) {
    eventManagers[remoteUserID]?.process(type)
  }

  /**
   * Report application event
   */
  fun reportEvent(type: AppEvent) {
    when (type) {
      is OnDominantSpeaker -> sender.send(DominantSpeakerEvent())
      is OnDeviceConnected -> sender.send(DeviceEvent(DeviceEvent.EVENT_CONNECTED, type.devices))
      is OnDeviceActive -> sender.send(DeviceEvent(DeviceEvent.EVENT_ACTIVE, type.devices))
    }
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
  fun sendUserFeedback(
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