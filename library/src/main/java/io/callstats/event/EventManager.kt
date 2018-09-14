package io.callstats.event

import io.callstats.CallstatsConfig
import io.callstats.OnIceConnectionChange
import io.callstats.OnStats
import io.callstats.PeerEvent
import io.callstats.event.fabric.FabricSetupEvent
import io.callstats.event.fabric.FabricTerminatedEvent
import io.callstats.interceptor.Interceptor
import io.callstats.utils.*
import org.webrtc.PeerConnection
import org.webrtc.RTCStatsReport
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
 * If it is WebRTC events, forward to interceptors and let them create events
 * If it is Application events, convert to event directly
 */
internal class EventManagerImpl(
    private val sender: EventSender,
    private val localID: String,
    private val remoteID: String,
    private val connection: PeerConnection,
    private val config: CallstatsConfig,
    private val interceptors: Array<Interceptor> = emptyArray()): EventManager
{
  internal var connectionID = ""
  private var statsTimer: Timer? = null

  override fun process(event: PeerEvent) {
    connection.getStats { report ->
      // every time ice connected, update connection ID
      if (event is OnIceConnectionChange && event.state == PeerConnection.IceConnectionState.CONNECTED) {
        connectionID = createConnectionID(report)
      }
      // no connection ID, don't send
      if (connectionID.isEmpty()) return@getStats

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

  private fun createConnectionID(report: RTCStatsReport): String {
    // create connection ID from local and remote candidate IP + Port
    val stats = report.statsMap
    return stats.selectedCandidatePairId()
        // find selected pair
        ?.let { selectId -> stats.candidatePairs().firstOrNull { it.id == selectId } }
        // find candidates and create ID
        ?.let { pair ->
          val local = stats.localCandidates().firstOrNull { it.id == pair.localCandidateId }
          val remote = stats.remoteCandidates().firstOrNull { it.id == pair.remoteCandidateId }
          if (local != null && remote != null) {
            md5("${local.ip}${local.port}${remote.ip}${remote.port}")
          } else null
        } ?: ""
  }
}