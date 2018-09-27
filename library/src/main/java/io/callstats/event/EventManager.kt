package io.callstats.event

import io.callstats.CallstatsConfig
import io.callstats.OnStats
import io.callstats.PeerEvent
import io.callstats.event.fabric.FabricSetupEvent
import io.callstats.event.fabric.FabricTerminatedEvent
import io.callstats.interceptor.Interceptor
import org.webrtc.PeerConnection
import java.util.Timer
import kotlin.concurrent.timerTask

/**
 * Manager than handle the events from WebRTC and Application
 */
internal interface EventManager {

  /**
   * Process peer events
   */
  fun process(event: PeerEvent)
}

/**
 * Forward to interceptors and let them create events
 */
internal class EventManagerImpl(
    private val sender: EventSender,
    private val localID: String,
    private val remoteID: String,
    private val connection: PeerConnection,
    private val config: CallstatsConfig,
    private val interceptors: Array<Interceptor> = emptyArray()): EventManager
{
  private val connectionID = createConnectionID()
  private var statsTimer: Timer? = null

  override fun process(event: PeerEvent) {
    connection.getStats { report ->
      // forward event
      interceptors.forEach { interceptor ->
        val events = interceptor.process(
            connection,
            event,
            localID,
            remoteID,
            connectionID,
            report.statsMap)
        events.forEach { sender.send(it) }
        events.firstOrNull { it is FabricSetupEvent } ?.also { startStatsTimer() }
        events.firstOrNull { it is FabricTerminatedEvent } ?.also { stopStatsTimer() }
      }
    }
  }

  private fun startStatsTimer() {
    stopStatsTimer()
    val period = config.statsSubmissionPeriod * 1000L
    statsTimer = Timer(true)
    statsTimer?.schedule(
        timerTask { process(OnStats) },
        period,
        period)
  }

  private fun stopStatsTimer() {
    statsTimer?.cancel()
    statsTimer = null
  }

  private fun createConnectionID(): String {
    return System.currentTimeMillis().toString()
  }
}