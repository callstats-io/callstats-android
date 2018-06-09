package io.callstats.demo

import android.os.Bundle
import android.provider.Settings
import android.support.v4.view.GravityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.view.WindowManager
import android.widget.ArrayAdapter
import io.callstats.Callstats
import io.callstats.demo.csiortc.CsioRTC
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import kotlinx.android.synthetic.main.activity_call.*
import kotlinx.android.synthetic.main.drawer_chat.*
import org.spongycastle.jce.provider.BouncyCastleProvider
import org.webrtc.VideoRenderer
import java.security.KeyFactory
import java.security.Security
import java.security.spec.PKCS8EncodedKeySpec

class CallActivity : AppCompatActivity(), CsioRTC.Callback {

  companion object {
    const val EXTRA_ROOM = "extra_room"

    // this is for Jwt creation for demo only
    init {
      Security.insertProviderAt(BouncyCastleProvider(), 1)
    }
  }

  private lateinit var csioRTC: CsioRTC
  private var peerIds = emptyArray<String>()

  // current renderer
  private var showingVideoFromPeer: String? = null
  private var currentVideoRenderer: VideoRenderer? = null

  // chat messages
  private val messageList = mutableListOf<String>()
  private lateinit var adapter: ArrayAdapter<String>

  // callstats
  private var callstats: Callstats? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_call)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    val name = "genius_murdock"
    val room = intent.getStringExtra(EXTRA_ROOM) ?: throw IllegalArgumentException("need room")

    startCallstats(room)

    // setup rtc
    csioRTC = CsioRTC(applicationContext, room, this, name)
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
    currentVideoRenderer = null

    stopCallstats()

    finish()
  }

  private fun showVideoFromPeerId(peerId: String) {
    if (peerId == showingVideoFromPeer) return
    // release previous renderer
    val peer = showingVideoFromPeer
    val renderer = currentVideoRenderer
    if (peer != null && renderer != null) {
      csioRTC.removeRemoteVideoRenderer(peer, renderer)
    }

    // add new renderer
    val newRenderer = VideoRenderer(remote_video_view)
    showingVideoFromPeer = peerId
    currentVideoRenderer = newRenderer
    csioRTC.addRemoteVideoRenderer(peerId, newRenderer)
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

  // Callstats Analytic

  private fun startCallstats(room: String) {
    // all of this Jwt creation should be done at server for security
    // this is just for demo only
    val appID = "710194177"
    val localID = System.currentTimeMillis().toString()
    val keyId = "d3d41c2f319f7f8dd6"

    // create jwt
    val key = """
      MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgSb3qUpRoMfNVt4+r
      9/QqHhyOrgid9g9ITQXKlCyD0juhRANCAARacoa2yLngi1vf6mhFJdORGA0oB3Zx
      NMWsSJNQDhcWXF3QBewOogBNprSyMTki1ldZXPJ3aL8ilZGwsBb1ojFZ
    """
    val factory = KeyFactory.getInstance("ECDSA", BouncyCastleProvider.PROVIDER_NAME)
    val keypem = factory.generatePrivate(PKCS8EncodedKeySpec(Base64.decode(key, Base64.DEFAULT)))

    val jwt = Jwts.builder()
        .setClaims(mapOf(
            "userID" to localID,
            "appID" to appID,
            "keyID" to keyId))
        .signWith(SignatureAlgorithm.ES256, keypem)
        .compact()

    callstats = Callstats(
        appID,
        localID,
        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID),
        jwt)
    callstats?.startSession(room)
  }

  private fun stopCallstats() {
    callstats?.stopSession()
  }
}