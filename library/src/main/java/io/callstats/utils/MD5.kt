package io.callstats.utils

import java.security.MessageDigest

fun md5(input: String): String {
  val digest = MessageDigest.getInstance("MD5")
  digest.update(input.toByteArray())
  val messageDigest = digest.digest()
  // Create Hex String
  val hexString = StringBuffer()
  for (i in 0 until messageDigest.size)
    hexString.append(Integer.toHexString(0xFF and messageDigest[i].toInt()))
  return hexString.toString()
}