package io.callstats.event

import io.callstats.CallstatsApplicationEvent
import io.callstats.CallstatsConfig
import io.callstats.CallstatsWebRTCEvent
import io.callstats.OnHold
import io.callstats.OnIceConnectionChange
import io.callstats.OnResume
import io.callstats.OnStats
import io.callstats.event.fabric.FabricActionEvent
import io.callstats.event.fabric.FabricSetupEvent
import io.callstats.event.fabric.FabricTerminatedEvent
import io.callstats.interceptor.Interceptor
import io.callstats.utils.candidatePairs
import io.callstats.utils.localCandidates
import io.callstats.utils.md5
import io.callstats.utils.remoteCandidates
import io.callstats.utils.selectedCandidatePairId
import org.webrtc.PeerConnection
import org.webrtc.RTCStatsReport
import java.util.Timer
import kotlin.concurrent.timerTask

/**
 * Manager than handle the stats incoming and forward to interceptors
 */
internal interface EventManager {
  fun process(webRTCEvent: CallstatsWebRTCEvent)
  fun process(applicationEvent: CallstatsApplicationEvent)
}

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

  override fun process(webRTCEvent: CallstatsWebRTCEvent) {
    connection.getStats { report ->
      // every time ice connected, update connection ID
      if (webRTCEvent is OnIceConnectionChange && webRTCEvent.state == PeerConnection.IceConnectionState.CONNECTED) {
        connectionID = createConnectionID(report)
      }
      // no connection ID, don't send
      if (connectionID.isEmpty()) return@getStats

      // forward event
      interceptors.forEach { interceptor ->
        val event = interceptor.process(
            connection,
            webRTCEvent,
            localID,
            remoteID,
            connectionID,
            report.statsMap)
        event.forEach { sender.send(it) }
        event.firstOrNull { it is FabricSetupEvent } ?.also { startStatsTimer() }
        event.firstOrNull { it is FabricTerminatedEvent } ?.also { stopStatsTimer() }
      }
    }
  }

  override fun process(applicationEvent: CallstatsApplicationEvent) {
    connectionID.takeIf { it.isNotEmpty() }
        ?.let { connId ->
          when (applicationEvent) {
            is OnHold -> sender.send(FabricActionEvent(remoteID, connId, FabricActionEvent.EVENT_HOLD))
            is OnResume -> sender.send(FabricActionEvent(remoteID, connId, FabricActionEvent.EVENT_RESUME))
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