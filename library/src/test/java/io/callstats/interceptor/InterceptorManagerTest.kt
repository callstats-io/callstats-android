package io.callstats.interceptor

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class InterceptorManagerTest {

  @Mock private lateinit var mockInterceptor1: Interceptor
  @Mock private lateinit var mockInterceptor2: Interceptor

  private lateinit var manager: InterceptorManager

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
    manager = InterceptorManager(arrayOf(mockInterceptor1, mockInterceptor2))
  }

  @Test
  fun forwardStatsToAllInterceptor() {
    manager.process("fabric", mapOf())
    verify(mockInterceptor1).process(eq("fabric"), any())
    verify(mockInterceptor2).process(eq("fabric"), any())
  }
}