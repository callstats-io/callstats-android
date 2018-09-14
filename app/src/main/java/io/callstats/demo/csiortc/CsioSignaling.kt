package io.callstats.demo.csiortc

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket

class CsioSignaling(
    private val room: String,
    private val callback: Callback,
    private val socket: Socket = IO.socket(URL))
{

  companion object {
    private const val URL = "https://demo.callstats.io"
    private const val TAG = "signaling"
    private const val EVENT_CONNECT = "connect"
    private const val EVENT_DISCONNECT = "disconnect"
    private const val EVENT_CONNECT_ERROR = "connect_error"
    private const val EVENT_JOIN = "join"
    private const val EVENT_LEAVE = "leave"
    private const val EVENT_MESSAGE = "message"
  }

  /**
   * Callback interface to listen to the signaling events
   */
  interface Callback {
    fun onConnect()
    fun onConnectError()
    fun onDisconnect()
    fun onPeerJoin(peerId: String)
    fun onPeerLeave(peerId: String)
    fun onMessage(fromId: String, message: String)
  }

  init {
    // listen to events

    socket.on(EVENT_CONNECT) {
      Log.i(TAG, "connected")
      socket.emit(EVENT_JOIN, room)
      callback.onConnect()
    }

    socket.on(EVENT_CONNECT_ERROR) {
      Log.i(TAG, "connect error")
      socket.close()
      callback.onConnectError()
    }

    socket.on(EVENT_JOIN) {
      val data = it[0] as String
      Log.i(TAG, "user joined : $data")
      callback.onPeerJoin(data)
    }

    socket.on(EVENT_LEAVE) {
      val data = it[0] as String
      Log.i(TAG, "user left : $data")
      callback.onPeerLeave(data)
    }

    socket.on(EVENT_MESSAGE) {
      val user = it[0] as String
      val msg = it[1] as String
      Log.i(TAG, "receive message from $user : $msg")
      callback.onMessage(user, msg)
    }

    socket.on(EVENT_DISCONNECT) {
      Log.i(TAG, "disconnected")
      callback.onDisconnect()
    }
  }

  /**
   * Join the room
   */
  fun start() {
    socket.connect()
  }

  /**
   * Leave the current room
   */
  fun stop() {
    callback.onDisconnect()
    socket.emit(EVENT_LEAVE)
    socket.disconnect()
    socket.off()
  }

  /**
   * Send a signalling message to another user
   */
  fun send(toId: String, message: String) {
    socket.emit(EVENT_MESSAGE, toId, message)
    Log.i(TAG, "sent message to $toId : $message")
  }
}