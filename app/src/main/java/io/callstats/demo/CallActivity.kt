package io.callstats.demo

import android.os.Bundle
import android.provider.Settings
import android.support.v4.view.GravityCompat
import android.support.v7.app.AppCompatActivity
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Toast
import io.callstats.demo.csiortc.CsioRTC
import kotlinx.android.synthetic.main.activity_call.*
import kotlinx.android.synthetic.main.drawer_chat.*

class CallActivity : AppCompatActivity(), CsioRTC.Callback {

  companion object {
    const val EXTRA_ROOM = "extra_room"
  }

  private lateinit var csioRTC: CsioRTC
  private var peerIds = emptyArray<String>()

  // current renderer
  private var showingVideoFromPeer: String? = null

  // chat messages
  private val messageList = mutableListOf<String>()
  private lateinit var adapter: ArrayAdapter<String>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_call)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    val name = "genius_murdock"
    val room = intent.getStringExtra(EXTRA_ROOM) ?: throw IllegalArgumentException("need room")

    // setup rtc
    csioRTC = CsioRTC(
        applicationContext,
        room,
        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID),
        this,
        name)
    local_video_view.init(csioRTC.localEglBase.eglBaseContext, null)
    remote_video_view.init(csioRTC.remoteEglBase.eglBaseContext, null)

    // self video should be mirrored
    local_video_view.setMirror(true)

    name_text.text = getString(R.string.call_my_name, name)
    count_text.text = getString(R.string.call_no_participant, 0)

    chat_button.setOnClickListener { drawer_layout.openDrawer(GravityCompat.END) }
    hang_button.setOnClickListener { finish() }

    mic_button.setOnClickListener {
      val selected = !it.isSelected
      it.isSelected = selected
      csioRTC.setMute(selected)
    }

    video_button.setOnClickListener {
      val selected = !it.isSelected
      it.isSelected = selected
      csioRTC.setVideoEnable(!selected)
    }

    left_button.setOnClickListener {
      showingVideoFromPeer?.let {
        val found = peerIds.indexOf(it)
        val index = if (found - 1 < 0) peerIds.size - 1 else found - 1
        showVideoFromPeerId(peerIds[index])
      }
    }

    right_button.setOnClickListener {
      showingVideoFromPeer?.let {
        val found = peerIds.indexOf(it)
        val index = if (found + 1 == peerIds.size) 0 else found + 1
        showVideoFromPeerId(peerIds[index])
      }
    }

    send_button.setOnClickListener {
      val input = chat_input.text.toString()
      if (input.isNotBlank()) {
        csioRTC.sendMessage(input)
        messageList.add("$name : $input")
        adapter.notifyDataSetChanged()
        chat_input.setText("")
      }
    }

    csioRTC.join()
    csioRTC.renderLocalVideo(local_video_view)

    // setup message list
    adapter = ArrayAdapter(this, R.layout.drawer_chat_message, messageList)
    list_view.adapter = adapter
  }

  override fun onStop() {
    super.onStop()

    csioRTC.leave()
    local_video_view.release()
    remote_video_view.release()

    finish()
  }

  private fun showVideoFromPeerId(peerId: String) {
    if (peerId == showingVideoFromPeer) return
    // release previous renderer
    val peer = showingVideoFromPeer
    if (peer != null) {
      csioRTC.removeRemoteVideoRenderer(peer, remote_video_view)
    }

    // add new renderer
    showingVideoFromPeer = peerId
    csioRTC.addRemoteVideoRenderer(peerId, remote_video_view)
  }

  // CsioRTC callback

  override fun onCsioRTCConnect() {}
  override fun onCsioRTCError() {}

  override fun onCsioRTCPeerUpdate() {
    runOnUiThread {
      // update no. of participants
      val peerIds = csioRTC.getPeerIds()
      count_text.text = getString(R.string.call_no_participant, peerIds.size)
      // save peer ids to navigate
      this.peerIds = peerIds
    }
  }

  override fun onCsioRTCPeerVideoAvailable() {
    runOnUiThread {
      val peerIds = csioRTC.getAvailableVideoPeerIds()
      if (showingVideoFromPeer == null && peerIds.isNotEmpty()) {
        showVideoFromPeerId(peerIds.first())
      }
    }
  }

  override fun onCsioRTCPeerMessage(peerId: String, message: String) {
    runOnUiThread {
      messageList.add("$peerId : $message")
      adapter.notifyDataSetChanged()
    }
  }

  override fun onCsioRTCDisconnect() {
    runOnUiThread {
      Toast.makeText(this, R.string.call_disconnect, Toast.LENGTH_SHORT).show()
      finish()
    }
  }
}