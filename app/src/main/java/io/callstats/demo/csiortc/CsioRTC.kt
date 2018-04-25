package io.callstats.demo.csiortc

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.webrtc.*

class CsioRTC(context: Context, room: String, val callback: Callback) : CsioSignaling.Callback {

  companion object {
    private const val TAG = "CsioRTC"
    private const val LOCAL_MEDIA_LABEL = "ARDAMS"
    private const val LOCAL_VIDEO_TRACK_LABEL = "ARDAMSv0"
    private const val LOCAL_AUDIO_TRACK_LABEL = "ARDAMSa0"

    private const val MESSAGE_ICE_KEY = "ice"
    private const val MESSAGE_OFFER_KEY = "offer"

    private const val LOCAL_VIDEO_WIDTH = 800
    private const val LOCAL_VIDEO_HEIGHT = 600
    private const val LOCAL_VIDEO_FPS = 30
  }

  interface Callback {
    fun onCsioRTCConnect()
    fun onCsioRTCError()
    fun onCsioRTCPeerUpdate()
    fun onCsioRTCPeerVideoAvailable()
  }

  private val signaling = CsioSignaling(room, this)
  private var localMediaStream: MediaStream? = null
  private var localVideoTrack: VideoTrack? = null
  private var localAudioTrack: AudioTrack? = null

  private val rootEglBase = EglBase.create()
  private val remoteRootEglBase = EglBase.create()
  private var peerConnections = mutableMapOf<String, PeerConnection>()
  private var peerVideoTracks = mutableMapOf<String, VideoTrack>()

  private val peerConnectionFactory: PeerConnectionFactory = {
    val opt = PeerConnectionFactory.InitializationOptions.builder(context)
        .createInitializationOptions()
    PeerConnectionFactory.initialize(opt)
    val factory = PeerConnectionFactory.builder().createPeerConnectionFactory()
    factory.setVideoHwAccelerationOptions(rootEglBase.eglBaseContext, remoteRootEglBase.eglBaseContext)
    factory
  }()

  /**
   * Join the room and start the call
   */
  fun join() {
    localMediaStream = peerConnectionFactory.createLocalMediaStream(LOCAL_MEDIA_LABEL)

    // audio
    val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
    localAudioTrack = peerConnectionFactory.createAudioTrack(LOCAL_AUDIO_TRACK_LABEL, audioSource)
    localMediaStream?.addTrack(localAudioTrack)

    // video
    Utils.createCameraCapturer()?.let {
      val videoSource = peerConnectionFactory.createVideoSource(it)
      it.startCapture(LOCAL_VIDEO_WIDTH, LOCAL_VIDEO_HEIGHT, LOCAL_VIDEO_FPS)
      localVideoTrack = peerConnectionFactory.createVideoTrack(LOCAL_VIDEO_TRACK_LABEL, videoSource)
      localMediaStream?.addTrack(localVideoTrack)
    }

    // start the flow
    signaling.start()
  }

  /**
   * Leave room and disconnect
   */
  fun leave() {
    signaling.stop()
    peerConnections.clear()
  }

  /**
   * Mute microphone
   */
  fun setMute(mute: Boolean) {
    localAudioTrack?.setEnabled(!mute)
  }

  /**
   * Enable or disable local video
   */
  fun setVideoEnable(enable: Boolean) {
    localVideoTrack?.setEnabled(enable)
  }

  // Video rendering

  /**
   * Render local video to surface view
   */
  fun renderLocalVideo(view: SurfaceViewRenderer) {
    localVideoTrack?.let {
      view.init(rootEglBase.eglBaseContext, null)
      view.setMirror(true)
      it.addRenderer(VideoRenderer(view))
    }
  }

  /**
   * Render remote video to surface view
   * Please note that this should be called in main thread only
   */
  fun renderRemoteVideo(peerId: String, view: SurfaceViewRenderer) {
    peerVideoTracks[peerId]?.let {
      view.init(remoteRootEglBase.eglBaseContext, null)
      it.addRenderer(VideoRenderer(view))
    }
  }

  // peers

  /**
   * Get current connected peer list
   */
  fun getPeerIds(): Array<String> {
    return peerConnections.keys.toTypedArray()
  }

  /**
   * Get peer list that each have video available
   */
  fun getAvailableVideoPeerIds(): Array<String> {
    return peerVideoTracks.keys.toTypedArray()
  }

  // Offer & Answer

  private fun offer(peerId: String) {
    val peerConnection = peerConnectionFactory.createPeerConnection(emptyList(), PeerObserver(peerId))
    peerConnection?.let {
      it.addStream(localMediaStream)
      peerConnections[peerId] = it
      it.createOffer(SdpObserver(peerId), MediaConstraints())
      callback.onCsioRTCPeerUpdate()
    }
  }

  private fun answer(peerId: String, offerSdp: SessionDescription) {
    val peerConnection = peerConnectionFactory.createPeerConnection(emptyList(), PeerObserver(peerId))
    peerConnection?.let {
      it.addStream(localMediaStream)
      peerConnections[peerId] = it
      val observer = SdpObserver(peerId)
      it.setRemoteDescription(observer, offerSdp)
      it.createAnswer(observer, MediaConstraints())
      callback.onCsioRTCPeerUpdate()
    }
  }

  // sdp observer

  private inner class SdpObserver(val peerId: String) : org.webrtc.SdpObserver {

    override fun onSetSuccess() {}
    override fun onSetFailure(reason: String?) {}

    override fun onCreateSuccess(sdp: SessionDescription) {
      peerConnections[peerId]?.setLocalDescription(this, sdp)
      val sdpJson = JSONObject()
      sdpJson.put("type", if (sdp.type == SessionDescription.Type.OFFER) "offer" else "answer")
      sdpJson.put("sdp", sdp.description)
      val json = JSONObject()
      json.put(MESSAGE_OFFER_KEY, sdpJson)
      signaling.send(peerId, json.toString())
    }

    override fun onCreateFailure(reason: String?) {}
  }

  // Peer observer

  private inner class PeerObserver(val peerId: String) : PeerConnection.Observer {
    override fun onIceCandidate(ice: IceCandidate) {
      val iceJson = JSONObject()
      iceJson.put("sdpMid", ice.sdpMid)
      iceJson.put("sdpMLineIndex", ice.sdpMLineIndex)
      iceJson.put("candidate", ice.sdp)
      val json = JSONObject()
      json.put(MESSAGE_ICE_KEY, iceJson)
      signaling.send(peerId, json.toString())
    }

    override fun onAddStream(stream: MediaStream) {
      stream.videoTracks?.firstOrNull()?.let {
        peerVideoTracks[peerId] = it
        callback.onCsioRTCPeerVideoAvailable()
      }
    }

    override fun onDataChannel(channel: DataChannel?) {}
    override fun onIceConnectionReceivingChange(p0: Boolean) {}
    override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
    override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
    override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
    override fun onRemoveStream(stream: MediaStream?) {}
    override fun onRenegotiationNeeded() {}
    override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
  }

  // Signaling callback

  override fun onConnect() {
    Log.i(TAG, "connected")
    callback.onCsioRTCConnect()
  }

  override fun onConnectError() {
    Log.i(TAG, "connect error")
    callback.onCsioRTCError()
  }

  override fun onPeerJoin(peerId: String) {
    offer(peerId)
  }

  override fun onPeerLeave(peerId: String) {
    peerVideoTracks.remove(peerId)
    peerConnections.remove(peerId)
    callback.onCsioRTCPeerUpdate()
  }

  override fun onMessage(fromId: String, message: String) {
    val json = JSONObject(message)
    // determine by key
    if (json.has(MESSAGE_ICE_KEY)) {
      peerConnections[fromId]?.let {
        val iceJson = json.getJSONObject(MESSAGE_ICE_KEY)
        val sdpMid = iceJson.getString("sdpMid")
        val sdpMLineIndex = iceJson.getInt("sdpMLineIndex")
        val sdp = iceJson.getString("candidate")
        val ice = IceCandidate(sdpMid, sdpMLineIndex, sdp)
        it.addIceCandidate(ice)
      }
    } else if (json.has(MESSAGE_OFFER_KEY)) {
      val offerJson = json.getJSONObject(MESSAGE_OFFER_KEY)
      val typeString = offerJson.getString("type")
      val type = if (typeString == "offer") SessionDescription.Type.OFFER else SessionDescription.Type.ANSWER
      val description = offerJson.getString("sdp")
      val sdp = SessionDescription(type, description)
      if (type == SessionDescription.Type.OFFER) {
        answer(fromId, sdp)
      } else if (type == SessionDescription.Type.ANSWER) {
        peerConnections[fromId]?.setRemoteDescription(SdpObserver(fromId), sdp)
      }
    }
  }
}