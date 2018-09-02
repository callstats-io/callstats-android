package io.callstats.interceptor

import io.callstats.OnIceConnectionChange
import io.callstats.OnStats
import io.callstats.PeerEvent
import io.callstats.event.Event
import io.callstats.event.stats.ConferenceStats
import org.webrtc.PeerConnection
import org.webrtc.RTCStats

/**
 * Interceptor to handle stats submission events
 */
internal class StatsInterceptor : Interceptor {

  private var startTimestamp = 0L
  private var lastSentTimestamp = 0L
  private val statCaches = mutableMapOf<String, CsioCache>()

  override fun process(
      connection: PeerConnection,
      event: PeerEvent,
      localID: String,
      remoteID: String,
      connectionID: String,
      stats: Map<String, RTCStats>): Array<Event>
  {
    // save start time
    if (event is OnIceConnectionChange
        && startTimestamp == 0L
        && event.state == PeerConnection.IceConnectionState.CONNECTED) {
      startTimestamp = System.currentTimeMillis()
    }

    if (event !is OnStats) return emptyArray()
    val currentTimestamp = System.currentTimeMillis()

    // convert to mutable list of stats
    val statsList = stats.values.map {
      mapOf(
          "id" to it.id,
          "type" to it.type,
          "timestamp" to it.timestampUs
      ).plus(it.members).toMutableMap()
    }

    // add csio internal stats
    statsList.forEach { stat ->
      val type = stat["type"]
      if (type == "inbound-rtp" || type == "outbound-rtp") {
        // create cache if not exist
        val id = stat["id"] as String
        val cache = statCaches[id] ?: CsioCache().also { statCaches[id] = it }

        sendCsioAvgBRKbps(stat, currentTimestamp)
        sendCsioIntBRKbps(stat, cache, currentTimestamp)

        // Outbound
        if (type == "outbound-rtp") {
          sendCsioAvgRtt(stat, cache)
          sendCsioIntMs(stat, currentTimestamp)
          sendCsioTimeElapseMs(stat, currentTimestamp)
        }
        // Inbound
        else if (type == "inbound-rtp") {
          sendCsioAvgJitter(stat, cache)
          sendCsioIntFLAndIntPktLoss(stat, cache)
        }
      }
    }

    // update states
    lastSentTimestamp = currentTimestamp
    return arrayOf(ConferenceStats(remoteID, connectionID, statsList.toTypedArray()))
  }

  private fun sendCsioAvgRtt(stat: MutableMap<String, Any>, cache: CsioCache) {
    (stat["roundTripTime"] as? Number)?.toDouble()?.let {
      cache.rttCount++
      cache.rttSum += it
      stat["csioAvgRtt"] = cache.rttSum / cache.rttCount
    }
  }

  private fun sendCsioAvgBRKbps(stat: MutableMap<String, Any>, currentTimestamp: Long) {
    if (startTimestamp != 0L) {
      val bytes = stat["bytesSent"] ?: stat["bytesReceived"]
      (bytes as? Number)?.toLong()?.let {
        stat["csioAvgBRKbps"] = (it * 8) / (currentTimestamp - startTimestamp)
      }
    }
  }

  private fun sendCsioIntBRKbps(stat: MutableMap<String, Any>, cache: CsioCache, currentTimestamp: Long) {
    if (lastSentTimestamp != 0L) {
      var cacheVal = 0L
      val bytes = (stat["bytesSent"] as? Number)
          ?.toLong()
          ?.also {
            cacheVal = cache.bytesSent
            cache.bytesSent = it
          } ?:
      (stat["bytesReceived"] as? Number)
          ?.toLong()
          ?.also {
            cacheVal = cache.bytesReceived
            cache.bytesReceived = it
          }
      bytes?.let {
        stat["csioIntBRKbps"] = (it - cacheVal) * 8 / (currentTimestamp - lastSentTimestamp)
      }
    }
  }

  private fun sendCsioIntMs(stat: MutableMap<String, Any>, currentTimestamp: Long) {
    if (lastSentTimestamp != 0L) {
      stat["csioIntMs"] = currentTimestamp - lastSentTimestamp
    }
  }

  private fun sendCsioTimeElapseMs(stat: MutableMap<String, Any>, currentTimestamp: Long) {
    if (startTimestamp != 0L) {
      stat["csioTimeElapseMs"] = currentTimestamp - startTimestamp
    }
  }

  private fun sendCsioAvgJitter(stat: MutableMap<String, Any>, cache: CsioCache) {
    (stat["jitter"] as? Number)?.toDouble()?.let {
      cache.jitterCount++
      cache.jitterSum += it
      stat["csioAvgJitter"] = cache.jitterSum / cache.jitterCount
    }
  }

  private fun sendCsioIntFLAndIntPktLoss(stat: MutableMap<String, Any>, cache: CsioCache) {
    val currentLostPackets = (stat["packetsLost"] as? Number)?.toLong()
    val currentReceivedPackets = (stat["packetsReceived"] as? Number)?.toLong()
    if (currentLostPackets != null && currentReceivedPackets != null) {
      val intLostPackets = currentLostPackets - cache.lostPackets
      val intReceivedPackets = currentReceivedPackets - cache.receivedPackets
      stat["csioIntFL"] = intLostPackets / (intLostPackets + intReceivedPackets)
      stat["csioIntPktLoss"] = intLostPackets
      cache.lostPackets = currentLostPackets
      cache.receivedPackets = currentReceivedPackets
    }
  }

  // cache values for calculations
  private class CsioCache {
    var rttSum = 0.0
    var rttCount = 0
    var jitterSum = 0.0
    var jitterCount = 0
    var bytesSent = 0L
    var bytesReceived = 0L
    var lostPackets = 0L
    var receivedPackets = 0L
  }
}