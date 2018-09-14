package io.callstats.event

import com.nhaarman.mockito_kotlin.whenever
import io.callstats.event.fabric.FabricTerminatedEvent
import io.callstats.event.user.UserAliveEvent
import io.callstats.event.user.UserJoinEvent
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mock
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import java.util.concurrent.ThreadPoolExecutor

class EventSenderTest {

  private lateinit var sender: EventSenderImpl

  @Mock private lateinit var client: OkHttpClient
  @Mock private lateinit var executor: ThreadPoolExecutor

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
    sender = EventSenderImpl(client, executor, "app1", "local1", "device1")
  }

  @Test
  fun eventHasCorrectInformation() {
    val event = object : Event() {}
    sender.send(event)
    assertEquals("local1", event.localID)
    assertEquals("device1", event.deviceID)
  }

  @Test
  fun eventWillNotOverwriteTimestamp() {
    val event = object : Event() {}
    event.timestamp = 300L
    sender.send(event)
    assertEquals(300L, event.timestamp)

    event.timestamp = 0L
    sender.send(event)
    assertNotEquals(0L, event.timestamp)
  }

  @Test
  fun sendEventBeforeNeededState() {
    sender.send(UserJoinEvent("con1"))
    sender.send(FabricTerminatedEvent("remote1", "con1"))
    assertEquals(1, sender.authenticatedQueue.size)
    assertEquals(1, sender.sessionQueue.size)
    verify(executor, never()).execute(any())
  }

  @Test
  fun sendEventsInCorrectOrder() {
    whenever(executor.execute(any()))
        .then {
          val runnable = (it.getArgument(0) as EventSendingRunnable)
          val map = when {
            runnable.event is AuthenticationEvent -> mapOf("access_token" to "1234")
            runnable.event is CreateSessionEvent -> mapOf("ucID" to "5678")
            else -> emptyMap()
          }
          runnable.callback.invoke(runnable.event, true, map)
        }

    // send event in reverse order
    sender.send(object : SessionEvent() {})
    sender.send(object : CreateSessionEvent() {})
    sender.send(object : Event(), AuthenticationEvent {
      override val code: String = "code"
      override val clientID: String = "clientID"
    })

    // verify that event sent in correct order
    val order = inOrder(executor)
    order.verify(executor).execute(argThat{ (it as EventSendingRunnable).event is AuthenticationEvent })
    order.verify(executor).execute(argThat{ (it as EventSendingRunnable).event is CreateSessionEvent })
    order.verify(executor).execute(argThat{ (it as EventSendingRunnable).event is SessionEvent })
  }

  @Test
  fun willNotSaveKeepAliveEvent() {
    sender.send(UserAliveEvent())
    assertEquals(0, sender.sessionQueue.size)
  }
}