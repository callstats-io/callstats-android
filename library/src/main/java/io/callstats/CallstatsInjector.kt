package io.callstats

import io.callstats.event.EventManager
import io.callstats.event.EventManagerImpl
import io.callstats.event.EventSender
import io.callstats.event.EventSenderImpl
import io.callstats.interceptor.FabricInterceptor
import io.callstats.interceptor.IceInterceptor
import io.callstats.interceptor.MediaInterceptor
import io.callstats.interceptor.SdpInterceptor
import io.callstats.interceptor.SsrcInterceptor
import io.callstats.interceptor.StatsInterceptor
import io.callstats.utils.SystemStatus
import io.callstats.utils.SystemStatusProvider
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.webrtc.PeerConnection
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal open class CallstatsInjector {

  open fun okhttpClient(): OkHttpClient = OkHttpClient.Builder()
      .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
      .build()

  open fun executor(): ExecutorService = Executors.newSingleThreadExecutor()

  open fun eventSender(
      client: OkHttpClient,
      executor: ExecutorService,
      appID: String,
      localID: String,
      deviceID: String): EventSender = EventSenderImpl(client, executor, appID, localID, deviceID)

  open fun eventManager(
      sender: EventSender,
      localID: String,
      remoteID: String,
      connection: PeerConnection,
      config: CallstatsConfig): EventManager
  {
    val interceptors = arrayOf(
        FabricInterceptor(),
        StatsInterceptor(),
        IceInterceptor(),
        SsrcInterceptor(),
        SdpInterceptor(),
        MediaInterceptor())
    return EventManagerImpl(sender, localID, remoteID, connection, config, interceptors)
  }

  open fun systemStatus(): SystemStatusProvider = SystemStatus()
}