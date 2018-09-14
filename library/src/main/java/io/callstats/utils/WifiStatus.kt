package io.callstats.utils

import android.content.Context
import android.net.wifi.WifiManager

internal interface WifiStatusProvider {
  fun wifiSignal(): Int?
  fun wifiRssi(): Int?
}

internal class WifiStatus(private val context: Context): WifiStatusProvider {

  private val wifiManager = lazy { context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager }

  override fun wifiSignal(): Int? {
    return wifiRssi()?.let {
      WifiManager.calculateSignalLevel(it, 100)
    }
  }

  override fun wifiRssi(): Int? {
    return try {
      wifiManager.value.connectionInfo.rssi
    } catch (ex: SecurityException) {
      null
    }
  }
}