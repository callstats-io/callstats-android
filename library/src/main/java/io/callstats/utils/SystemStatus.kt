package io.callstats.utils

import android.content.Context
import java.io.RandomAccessFile
import android.os.BatteryManager
import android.content.Intent
import android.content.IntentFilter
import android.content.Context.ACTIVITY_SERVICE
import android.app.ActivityManager

/**
 * Provide system information
 */
internal interface SystemStatusProvider {
  fun cpuLevel(): Int?
  fun batteryLevel(context: Context): Int?
  fun availableMemory(context: Context): Int?
  fun usageMemory(context: Context): Int?
  fun threadCount(): Int?
}

internal class SystemStatus: SystemStatusProvider {

  // https://issuetracker.google.com/issues/37140047
  override fun cpuLevel(): Int? {
    return try {
      val reader = RandomAccessFile("/proc/stat", "r")
      val load = reader.readLine()
      val info = load.split(" ")
      val cpuIdle = info[4].toDouble() + info[5].toDouble()
      val cpuAll = (1..8).map { info[it].toDouble() }.reduce(Double::plus)
      (((cpuAll - cpuIdle) / cpuAll) * 100).toInt()
    } catch (ex: Exception) {
      null
    }
  }

  override fun batteryLevel(context: Context): Int? {
    return try {
      val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
      val batteryStatus = context.registerReceiver(null, iFilter)
      val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
      val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
      val batteryPct = level / scale.toFloat()
      (batteryPct * 100).toInt()
    } catch (ex: Exception) {
      null
    }
  }

  override fun availableMemory(context: Context): Int? {
    return try {
      val mi = ActivityManager.MemoryInfo()
      val activityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
      activityManager.getMemoryInfo(mi)
      val mb = mi.totalMem / 0x100000L
      mb.toInt()
    } catch (ex: Exception) {
      null
    }
  }

  override fun usageMemory(context: Context): Int? {
    return try {
      val mi = ActivityManager.MemoryInfo()
      val activityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
      activityManager.getMemoryInfo(mi)
      val mb = (mi.totalMem - mi.availMem) / 0x100000L
      mb.toInt()
    } catch (ex: Exception) {
      null
    }
  }

  override fun threadCount(): Int? {
    return Thread.getAllStackTraces().keys.size
  }
}