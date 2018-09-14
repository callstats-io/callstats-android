package io.callstats.event.info

import io.callstats.utils.ssrcValues
import org.webrtc.PeerConnection
import org.webrtc.RTCStats

/**
 * SSRC info
 */
internal data class Ssrc(
    val ssrc: String,
    val cname: String,
    val streamType: String,
    val reportType: String,
    val mediaType: String,
    val userID: String,
    val msid: String,
    val mslabel: String,
    val label: String,
    val localStartTime: Double) {

  companion object {
    const val REPORT_LOCAL = "local"
    const val REPORT_REMOTE = "remote"

    fun fromStats(stats: RTCStats, connection: PeerConnection, localId: String, remoteId: String): Ssrc? {
      val isRemote = stats.members["isRemote"] as? Boolean ?: return null
      val sdp = if (!isRemote) connection.localDescription else connection.remoteDescription
      val id = stats.members["ssrc"] as? String ?: return null
      val values = sdp.ssrcValues(id) ?: return null
      val cname = values["cname"] ?: return null
      val msid = values["msid"] ?: return null
      val mslabel = values["mslabel"] ?: return null
      val label = values["label"] ?: return null
      val mediaType = stats.members["mediaType"] as? String ?: return null
      val streamType = stats.type.replace("-rtp", "")
      return Ssrc(
          id,
          cname,
          streamType,
          if (!isRemote) REPORT_LOCAL else REPORT_REMOTE,
          mediaType,
          if (!isRemote) localId else remoteId,
          msid,
          mslabel,
          label,
          stats.timestampUs)
    }
  }
}