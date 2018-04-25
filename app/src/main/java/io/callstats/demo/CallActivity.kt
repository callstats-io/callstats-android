package io.callstats.demo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.callstats.demo.csiortc.CsioRTC
import kotlinx.android.synthetic.main.activity_call.*

class CallActivity : AppCompatActivity(), CsioRTC.Callback {

  companion object {
    const val EXTRA_ROOM = "extra_room"
  }

  private var csioRTC: CsioRTC? = null
  private var showingVideoFromPeer: String? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_call)

    name_text.text = getString(R.string.call_my_name, "genius_murdock")
    count_text.text = getString(R.string.call_no_participant, 0)

    mic_button.setOnClickListener {
      val selected = !it.isSelected
      it.isSelected = selected
      csioRTC?.setMute(selected)
    }

    video_button.setOnClickListener {
      val selected = !it.isSelected
      it.isSelected = selected
      csioRTC?.setVideoEnable(!selected)
    }

    hang_button.setOnClickListener { finish() }
  }

  override fun onStart() {
    super.onStart()
    val room = intent.getStringExtra(EXTRA_ROOM) ?: throw IllegalArgumentException("need room")
    csioRTC = CsioRTC(applicationContext, room, this)
    csioRTC?.join()
    csioRTC?.renderLocalVideo(local_video_view)
  }

  override fun onStop() {
    super.onStop()
    csioRTC?.leave()
    finish()
  }

  // CsioRTC callback

  override fun onCsioRTCConnect() {}
  override fun onCsioRTCError() {}

  override fun onCsioRTCPeerUpdate() {
    runOnUiThread {
      csioRTC?.let {
        // update no. of participants
        val peerIds = it.getPeerIds()
        count_text.text = getString(R.string.call_no_participant, peerIds.size)
      }
    }
  }

  override fun onCsioRTCPeerVideoAvailable() {
    runOnUiThread {
      csioRTC?.let {
        val peerIds = it.getAvailableVideoPeerIds()
        if (showingVideoFromPeer == null && peerIds.isNotEmpty()) {
          val peerId = peerIds.first()
          it.renderRemoteVideo(peerId, remote_video_view)
          showingVideoFromPeer = peerId
        }
      }
    }
  }
}