package io.callstats.event

import io.callstats.CallstatsApplicationEvent
import io.callstats.CallstatsConfig
import io.callstats.CallstatsMediaActionEvent
import io.callstats.CallstatsWebRTCEvent
import io.callstats.OnAudio
import io.callstats.OnHold
import io.callstats.OnIceConnectionChange
import io.callstats.OnResume
import io.callstats.OnScreenShare
import io.callstats.OnStats
import io.callstats.OnVideo
import io.callstats.event.fabric.FabricActionEvent
import io.callstats.event.fabric.FabricSetupEvent
import io.callstats.event.fabric.FabricTerminatedEvent
import io.callstats.event.media.MediaActionEvent
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
 * Manager than handle the events from WebRTC and Application
 */
internal interface EventManager {

  /**
   * Process WebRTC events
   */
  fun process(event: CallstatsWebRTCEvent)

  /**
   * Process Application events
   */
  fun process(event: CallstatsApplicationEvent)
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

  override fun process(event: CallstatsWebRTCEvent) {
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

  override fun process(event: CallstatsApplicationEvent) {
    connectionID.takeIf { it.isNotEmpty() }
        ?.let { connId ->
          when (event) {
            is OnHold -> sender.send(FabricActionEvent(remoteID, connId, FabricActionEvent.EVENT_HOLD))
            is OnResume -> sender.send(FabricActionEvent(remoteID, connId, FabricActionEvent.EVENT_RESUME))
            is CallstatsMediaActionEvent -> {
              val eventType = when (event) {
                is OnAudio -> if (event.mute) MediaActionEvent.EVENT_MUTE else MediaActionEvent.EVENT_UNMUTE
                is OnVideo -> if (event.enable) MediaActionEvent.EVENT_VIDEO_RESUME else MediaActionEvent.EVENT_VIDEO_PAUSE
                is OnScreenShare -> if (event.enable) MediaActionEvent.EVENT_SCREENSHARE_START else MediaActionEvent.EVENT_SCREENSHARE_STOP
              }
              sender.send(MediaActionEvent(remoteID, connId, eventType, event.mediaDeviceID))
            }
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