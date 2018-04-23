package io.callstats.demo.csiortc

import android.util.Log
import org.webrtc.Camera1Enumerator
import org.webrtc.VideoCapturer

class Utils {

  companion object {
    private const val TAG = "Utils"

    /**
     * Find the camera capturer
     */
    fun createCameraCapturer(): VideoCapturer? {
      val enumerator = Camera1Enumerator(false)
      val deviceNames = enumerator.deviceNames

      // First, try to find front facing camera
      Log.d(TAG, "Looking for front facing cameras.")
      for (deviceName in deviceNames) {
        if (enumerator.isFrontFacing(deviceName)) {
          Log.d(TAG, "Creating front facing camera capturer.")
          val videoCapturer = enumerator.createCapturer(deviceName, null)

          if (videoCapturer != null) {
            return videoCapturer
          }
        }
      }

      // Front facing camera not found, try something else
      Log.d(TAG, "Looking for other cameras.")
      for (deviceName in deviceNames) {
        if (!enumerator.isFrontFacing(deviceName)) {
          Log.d(TAG, "Creating other camera capturer.")
          val videoCapturer = enumerator.createCapturer(deviceName, null)

          if (videoCapturer != null) {
            return videoCapturer
          }
        }
      }

      return null
    }
  }
}