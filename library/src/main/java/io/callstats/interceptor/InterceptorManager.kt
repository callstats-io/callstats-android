package io.callstats.interceptor

/**
 * Manager than handle the stats incoming and forward to interceptors
 */
internal class InterceptorManager(private val interceptors: Array<Interceptor>) {

  fun process(type: String, stats: Map<String, Any>) {
    interceptors.forEach { it.process(type, stats) }
  }
}