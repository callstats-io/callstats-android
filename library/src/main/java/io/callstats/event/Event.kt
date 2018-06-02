package io.callstats.event

/**
 * Base event
 */
open class Event {
  internal var localID: String = ""
  internal var deviceID: String = ""
  internal var timestamp: Long = 0L
  internal var originID: String? = null
}