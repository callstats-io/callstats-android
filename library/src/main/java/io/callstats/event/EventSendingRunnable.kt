package io.callstats.event

import com.google.gson.Gson
import okhttp3.OkHttpClient
import com.google.gson.reflect.TypeToken

/**
 * Runnable class to send the event in thread pool executor
 * @param client OkHttpClient
 * @param event Event to be sent
 * @param gson Gson to convert between json string and object
 */
class EventSendingRunnable(
    private val client: OkHttpClient,
    internal val event: Event,
    private val gson: Gson) : Runnable {

  var callback: (Event, Boolean, Map<String, Any?>?) -> Unit = { _, _, _ -> }

  override fun run() {
    try {
      val request = event.toRequest(gson)
      val response = client.newCall(request).execute()
      val type = object : TypeToken<Map<String, Any?>>() {}.type
      val map: Map<String, Any?> = response.body()?.string().let { gson.fromJson(it, type) }
      callback(event, response.isSuccessful, map)
      response.close()
    } catch (ex: Exception) {
      println(ex.localizedMessage)
      callback(event, false, null)
    }
  }
}