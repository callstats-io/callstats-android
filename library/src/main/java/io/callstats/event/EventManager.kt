package io.callstats.event

import io.callstats.CallstatsWebRTCFunction
import io.callstats.interceptor.FabricInterceptor
import io.callstats.interceptor.Interceptor
import io.callstats.utils.md5
import org.webrtc.PeerConnection
import org.webrtc.RTCStatsReport

/**
 * Manager than handle the stats incoming and forward to interceptors
 * @param sender [EventSender] to send event produce by [Interceptor]
 */
internal open class EventManager(
    private val sender: EventSender,
    private val remoteID: String,
    private val connection: PeerConnection,
    private val interceptors: Array<Interceptor> = arrayOf(FabricInterceptor(remoteID))) {

  private var connectionID = ""

  fun process(webRTCEvent: CallstatsWebRTCFunction) {
    connection.getStats { report ->
      // create connection id
      if (connectionID.isEmpty()) connectionID = createConnectionID(report)

      // forward event
      interceptors.forEach { interceptor ->
        val event = interceptor.process(webRTCEvent, connectionID, report.statsMap)
        event?.let { sender.send(event) }
      }
    }
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