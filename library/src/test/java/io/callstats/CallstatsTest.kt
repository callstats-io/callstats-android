package io.callstats

import android.content.Context
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.argWhere
import com.nhaarman.mockito_kotlin.timeout
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.callstats.event.CreateSessionEvent
import io.callstats.event.EventManager
import io.callstats.event.EventSender
import io.callstats.event.KeepAliveEvent
import io.callstats.event.auth.TokenRequest
import io.callstats.event.fabric.FabricSetupFailedEvent
import io.callstats.event.special.LogEvent
import io.callstats.event.stats.SystemStatusStats
import io.callstats.event.user.UserLeftEvent
import io.callstats.utils.SystemStatusProvider
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.webrtc.PeerConnection
import java.util.concurrent.ExecutorService

class CallstatsTest {

  @Mock private lateinit var context: Context
  @Mock private lateinit var sender: EventSender
  @Mock private lateinit var connection: PeerConnection
  @Mock private lateinit var manager: EventManager
  @Mock private lateinit var systemStatus: SystemStatusProvider

  private lateinit var callstats: Callstats
  private lateinit var defaultConfig: CallstatsConfig

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)

    // use mock as sender dependency
    Callstats.dependency = object : Callstats.Dependency() {
      override fun eventSender(
          client: OkHttpClient,
          executor: ExecutorService,
          appID: String,
          localID: String,
          deviceID: String): EventSender {
        return sender
      }

      override fun eventManager(sender: EventSender, remoteID: String, connection: PeerConnection, config: CallstatsConfig): EventManager {
        return manager
      }

      override fun systemStatus(): SystemStatusProvider {
        return systemStatus
      }
    }

    // config for testing
    defaultConfig = CallstatsConfig().apply {
      keepAlivePeriod = 5
      statsSubmissionPeriod = 5
    }
    callstats = Callstats(context, "app1", "local1", "device1", "code", "1.0", defaultConfig)
  }

  @Test
  fun createObjectWillDoAuthentication() {
    verify(sender).send(any<TokenRequest>())
  }

  @Test
  fun startSessionSendSessionCreateEvent() {
    callstats.startSession("conf1")
    verify(sender).send(any<CreateSessionEvent>())
  }

  @Test
  fun startSessionAlsoStartSendingKeepAlive() {
    callstats.startSession("conf1")
    val waitingTime = (defaultConfig.keepAlivePeriod + 5) * 1000L // wait more 5 sec
    verify(sender, timeout(waitingTime)).send(any<KeepAliveEvent>())
  }

  @Test
  fun stopSessionSendUserLeftEvent() {
    callstats.stopSession()
    verify(sender).send(any<UserLeftEvent>())
  }

  @Test
  fun startSessionAlsoStartSendingSystemStats() {
    whenever(systemStatus.cpuLevel()).thenReturn(0)
    whenever(systemStatus.availableMemory(anyOrNull())).thenReturn(0)
    whenever(systemStatus.usageMemory(anyOrNull())).thenReturn(0)
    whenever(systemStatus.batteryLevel(anyOrNull())).thenReturn(0)
    whenever(systemStatus.threadCount()).thenReturn(0)
    callstats.startSession("conf1")
    val waitingTime = (defaultConfig.systemStatsSubmissionPeriod + 5) * 1000L // wait more 5 sec
    verify(sender, timeout(waitingTime)).send(any<SystemStatusStats>())
  }

  @Test
  fun stopSessionAlsoStopSendingSystemStats() {
    whenever(systemStatus.cpuLevel()).thenReturn(0)
    whenever(systemStatus.availableMemory(anyOrNull())).thenReturn(0)
    whenever(systemStatus.usageMemory(anyOrNull())).thenReturn(0)
    whenever(systemStatus.batteryLevel(anyOrNull())).thenReturn(0)
    whenever(systemStatus.threadCount()).thenReturn(0)
    callstats.startSession("conf1")
    callstats.stopSession()
    val waitingTime = (defaultConfig.systemStatsSubmissionPeriod + 5) * 1000L // wait more 5 sec
    verify(sender, timeout(waitingTime).times(0)).send(any<SystemStatusStats>())
  }

  @Test
  fun addNewFabricCreateEventManager() {
    callstats.addNewFabric(connection, "remote1")
    callstats.addNewFabric(connection, "remote1") // duplicate should not create new manager
    assertEquals(1, callstats.eventManagers.size)
  }

  @Test
  fun reportEventSendThroughManager() {
    callstats.addNewFabric(connection, "remote1")
    callstats.reportEvent(
        "remote1",
        OnIceConnectionChange(PeerConnection.IceConnectionState.CONNECTED))
    verify(manager).process(any())
  }

  @Test
  fun reportErrorSendValidEvent() {
    callstats.reportError(CallstatsError.MEDIA_PERMISSION, "msg1", "stack1")
    verify(sender).send(argWhere {
      it is FabricSetupFailedEvent
          && it.reason == CallstatsError.MEDIA_PERMISSION.value
          && it.message == "msg1"
          && it.stack == "stack1"
    })
  }

  @Test
  fun loggingSendValidEvent() {
    callstats.log("msg")
    verify(sender).send(argWhere {
      it is LogEvent && it.message == "msg" && it.level == "info" && it.messageType == "text"
    })
  }
}