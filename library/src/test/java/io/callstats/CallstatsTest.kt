package io.callstats

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import io.callstats.event.EventSender
import io.callstats.event.auth.TokenRequest
import io.callstats.event.user.UserJoinEvent
import io.callstats.event.user.UserLeftEvent
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.ExecutorService

class CallstatsTest {

  @Mock private lateinit var sender: EventSender

  private lateinit var callstats: Callstats

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
    }
    callstats = Callstats("app1", "local1", "device1", "code")
  }

  @Test
  fun createObjectWillDoAuthentication() {
    verify(sender).send(any<TokenRequest>())
  }

  @Test
  fun startSessionSendUserJoinEvent() {
    callstats.startSession("conf1")
    verify(sender).send(any<UserJoinEvent>())
  }

  @Test
  fun stopSessionSendUserLeftEvent() {
    callstats.stopSession()
    verify(sender).send(any<UserLeftEvent>())
  }
}