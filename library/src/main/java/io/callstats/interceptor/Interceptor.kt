package io.callstats.interceptor

/**
 * The interceptor to process the event sent by app
 */
internal interface Interceptor {

  /**
   * Process the incoming type and data stats
   */
  fun process(type: String, stats: Map<String, Any>)
}