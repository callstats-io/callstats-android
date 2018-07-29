package io.callstats.event.special

import io.callstats.event.SessionEvent

/**
 * You can submit application error logs using this event. You will be able to search for them and also categorize them.
 *
 * @param level "debug" "info" "warn" "error" "fatal"
 * @param message application message
 * @param messageType "text" "json" "domError"
 */
class LogEvent(
    val level: String,
    val message: String,
    val messageType: String) : SessionEvent()
{
  override fun path() = "events/app/logs"
}