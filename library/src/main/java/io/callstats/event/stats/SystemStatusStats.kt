package io.callstats.event.stats

import io.callstats.event.AuthenticatedEvent

/**
 * Submitting System status information for a conference such as battery level, cpu , etc.
 */
class SystemStatusStats: AuthenticatedEvent() {

  /**
   * CPU level in percentage
   */
  var cpuLevel: Int? = null

  /**
   * Battery level in percentage
   */
  var batteryLevel: Int? = null

  /**
   * Memory usage in MB
   */
  var memoryUsage: Int? = null

  /**
   * Total memory in MB
   */
  var memoryAvailable: Int? = null

  /**
   * Number of threads
   */
  var threadCount: Int? = null

  /**
   * check if this stats has value to be sent
   */
  fun isValid() = cpuLevel ?: batteryLevel ?: memoryUsage ?: memoryAvailable ?: threadCount != null

  override fun url() = "https://stats.callstats.io"
  override fun path() = "stats/system"
}