package io.callstats.event.info

import android.os.Build
import io.callstats.BuildConfig

data class EndpointInfo(val appVersion: String? = null) {
  val type = "native"
  val os = "Android"
  val osVersion = Build.VERSION.RELEASE
  val buildVersion = BuildConfig.VERSION_NAME
}