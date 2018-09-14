package io.callstats.event.info

/**
 * Media device info
 *
 * @param kind "audioinput" "audiooutput" "videoinput"
 * @param groupID Group identifier of the device (note: two devices belong to the same group identifier only if they belong to the same physical device)
 */
data class MediaDevice(val kind: String, val groupID: String) {

  /**
   * It is obtained from mediaDevices.enumerateDevices() API or similar API in native clients
   */
  var mediaDeviceID: String? = null

  /**
   * Input device name (example: external USB Webcam)
   */
  var label: String? = null
}