package io.callstats.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class MD5Test {

  @Test
  fun correctValue() {
    val input = "callstats.io"
    val output = md5(input)
    assertEquals("63e49c437c8c10c145e85b271e94f9d0", output)
  }
}