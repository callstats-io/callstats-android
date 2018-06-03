package io.callstats.event

import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Runnable class to send the event in thread pool executor
 * @param client OkHttpClient
 * @param request request object to be sent
 */
class EventSendingRunnable(
    private val client: OkHttpClient,
    private val request: Request) : Runnable {

  var callback: (Boolean, String?) -> Unit = { _, _ -> }

  override fun run() {
    try {
      val response = client.newCall(request).execute()
      callback(response.isSuccessful, response.body()?.string())
      response.close()
    } catch (ex: Exception) {
      println(ex.localizedMessage)
      callback(false, null)
    }
  }
}