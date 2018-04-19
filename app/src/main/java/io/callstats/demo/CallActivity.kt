package io.callstats.demo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.callstats.demo.csiortc.CsioSignaling

class CallActivity : AppCompatActivity() {

  companion object {
    const val EXTRA_ROOM = "extra_room"
    const val EXTRA_USER = "extra_user"
  }

  private var signaling: CsioSignaling? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_call)

    val room = intent.getStringExtra(EXTRA_ROOM) ?: throw IllegalArgumentException("need room")
    val user = intent.getStringExtra(EXTRA_USER) ?: throw IllegalArgumentException("need user")
    start(room, user)
  }

  fun start(room: String, user: String) {
    signaling = CsioSignaling(user, object : CsioSignaling.Callback {
      override fun onConnect() {
        signaling?.start(room)
      }

      override fun onConnectError() {

      }

      override fun onPeerJoin(peerId: String) {
      }

      override fun onPeerLeave(peerId: String) {
      }

      override fun onMessage(fromId: String, message: String) {
      }

    })
  }
}