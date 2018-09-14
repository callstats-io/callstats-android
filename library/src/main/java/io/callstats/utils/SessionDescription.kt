package io.callstats.utils

import org.webrtc.SessionDescription

/**
 * Extract SSRC values from ID in session description
 */
internal fun SessionDescription.ssrcValues(id: String): Map<String, String>? {
  var values: HashMap<String, String>? = null
  val lines = description.split("\\r?\\n".toRegex())
  val prefix = "a=ssrc:$id "
  for (line in lines) {
    if (line.startsWith(prefix)) {
      if (values == null) values = HashMap()
      val array = line.replace(prefix, "").split(":")
      values[array[0]] = array[1]
    }
  }
  return values
}