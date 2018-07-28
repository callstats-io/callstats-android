package io.callstats.event

import io.callstats.CallstatsConfig
import io.callstats.CallstatsWebRTCFunction
import io.callstats.OnStats
import io.callstats.event.fabric.FabricSetupEvent
import io.callstats.event.fabric.FabricTerminatedEvent
import io.callstats.interceptor.FabricInterceptor
import io.callstats.interceptor.IceInterceptor
import io.callstats.interceptor.Interceptor
import io.callstats.interceptor.StatsInterceptor
import io.callstats.utils.md5
import org.webrtc.PeerConnection
import org.webrtc.RTCStatsReport
import java.util.Timer
import kotlin.concurrent.timerTask

/**
 * Manager than handle the stats incoming and forward to interceptors
 */
internal interface EventManager {
  fun process(webRTCEvent: CallstatsWebRTCFunction)
}

internal class EventManagerImpl(
    private val sender: EventSender,
    private val remoteID: String,
    private val connection: PeerConnection,
    private val config: CallstatsConfig,
    private val interceptors: Array<Interceptor> = arrayOf(
        FabricInterceptor(remoteID),
        StatsInterceptor(remoteID),
        IceInterceptor(remoteID))): EventManager
{
  private var connectionID = ""
  private var statsTimer: Timer? = null

  override fun process(webRTCEvent: CallstatsWebRTCFunction) {
    connection.getStats { report ->
      // create connection id
      if (connectionID.isEmpty()) connectionID = createConnectionID(report)

      // forward event
      interceptors.forEach { interceptor ->
        val event = interceptor.process(webRTCEvent, connectionID, report.statsMap)
        event.forEach { sender.send(it) }
        event.firstOrNull { it is FabricSetupEvent } ?.also { startStatsTimer() }
        event.firstOrNull { it is FabricTerminatedEvent } ?.also { stopStatsTimer() }
      }
    }
  }

  private fun startStatsTimer() {
    stopStatsTimer()
    val period = config.statsSubmissionPeriod * 1000L
    statsTimer = Timer(true)
    statsTimer?.schedule(
        timerTask { process(OnStats()) },
        period,
        period)
  }

  private fun stopStatsTimer() {
    statsTimer?.cancel()
    statsTimer = null
  }

  private fun createConnectionID(report: RTCStatsReport): String {
    // create connection ID from local and remote candidate IP + Port
    val candidates = report.statsMap.values.filter { it.type == "local-candidate" || it.type == "remote-candidate" }
    if (candidates.size != 2) return ""
    val local = candidates.firstOrNull { it.type == "local-candidate" }?.members ?: return ""
    val remote = candidates.firstOrNull { it.type == "remote-candidate" }?.members ?: return ""
    return md5("${local["ip"]}${local["port"]}${remote["ip"]}${remote["port"]}")
  }
}