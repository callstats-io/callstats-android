package io.callstats.event.device

import io.callstats.event.SessionEvent
import io.callstats.event.info.MediaDevice

/**
 * Information about the connected and/or active media devices.
 *
 * @param eventType "connectedDeviceList" "activeDeviceList"
 * @param mediaDeviceList list of devices
 */
class DeviceEvent(val eventType: String, val mediaDeviceList: Array<MediaDevice>) : SessionEvent() {

  companion object {
    const val EVENT_CONNECTED = "connectedDeviceList"
    const val EVENT_ACTIVE = "activeDeviceList"
  }
}