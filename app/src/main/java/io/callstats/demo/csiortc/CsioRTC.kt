package io.callstats.demo.csiortc

import android.content.Context
import android.util.Base64
import android.util.Log
import io.callstats.Callstats
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.json.JSONObject
import org.spongycastle.jce.provider.BouncyCastleProvider
import org.webrtc.*
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.Security
import java.security.spec.PKCS8EncodedKeySpec

class CsioRTC(
    context: Context,
    private val room: String,
    deviceID: String,
    val callback: Callback,
    private val alias: String? = null) : CsioSignaling.Callback {

  companion object {
    private const val TAG = "CsioRTC"
    private const val LOCAL_MEDIA_LABEL = "ARDAMS"
    private const val LOCAL_VIDEO_TRACK_LABEL = "ARDAMSv0"
    private const val LOCAL_AUDIO_TRACK_LABEL = "ARDAMSa0"
    private const val DATA_CHANNEL_LABEL = "chat"
    private const val DATA_CHANNEL_NAME_KEY = "aliseName"
    private const val DATA_CHANNEL_MESSAGE_KEY = "message"

    private const val MESSAGE_ICE_KEY = "ice"
    private const val MESSAGE_OFFER_KEY = "offer"

    private const val LOCAL_VIDEO_WIDTH = 800
    private const val LOCAL_VIDEO_HEIGHT = 600
    private const val LOCAL_VIDEO_FPS = 30

    // this is for Jwt creation for demo only
    init {
      Security.insertProviderAt(BouncyCastleProvider(), 1)
    }
  }

  interface Callback {
    fun onCsioRTCConnect()
    fun onCsioRTCDisconnect()
    fun onCsioRTCError()
    fun onCsioRTCPeerUpdate()
    fun onCsioRTCPeerVideoAvailable()
    fun onCsioRTCPeerMessage(peerId: String, message: String)
  }

  private val signaling = CsioSignaling(room, this)
  private var localMediaStream: MediaStream? = null
  private var localVideoCapturer: VideoCapturer? = null
  private var localVideoSource: VideoSource? = null
  private var localVideoTrack: VideoTrack? = null
  private var localAudioSource: AudioSource? = null
  private var localAudioTrack: AudioTrack? = null

  val localEglBase: EglBase = EglBase.create()
  val remoteEglBase: EglBase = EglBase.create()

  private var peerConnections = mutableMapOf<String, PeerConnection>()
  private var peerVideoTracks = mutableMapOf<String, VideoTrack>()
  private var peerDataChannels = mutableMapOf<String, DataChannel>()

  private val peerConnectionFactory: PeerConnectionFactory = {
    val opt = PeerConnectionFactory.InitializationOptions.builder(context)
        .createInitializationOptions()
    PeerConnectionFactory.initialize(opt)
    val factory = PeerConnectionFactory.builder().createPeerConnectionFactory()
    factory.setVideoHwAccelerationOptions(localEglBase.eglBaseContext, remoteEglBase.eglBaseContext)
    factory
  }()

  // callstats
  private val callstats: Callstats

  init {
    callstats = initCallstats(deviceID)
  }

  /**
   * Join the room and start the call
   */
  fun join() {
    startCallstats(room)

    localMediaStream = peerConnectionFactory.createLocalMediaStream(LOCAL_MEDIA_LABEL)

    // audio
    val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
    localAudioTrack = peerConnectionFactory.createAudioTrack(LOCAL_AUDIO_TRACK_LABEL, audioSource)
    localMediaStream?.addTrack(localAudioTrack)
    localAudioSource = audioSource

    // video
    Utils.createCameraCapturer()?.let {
      val videoSource = peerConnectionFactory.createVideoSource(it)
      it.startCapture(LOCAL_VIDEO_WIDTH, LOCAL_VIDEO_HEIGHT, LOCAL_VIDEO_FPS)
      localVideoTrack = peerConnectionFactory.createVideoTrack(LOCAL_VIDEO_TRACK_LABEL, videoSource)
      localMediaStream?.addTrack(localVideoTrack)
      localVideoCapturer = it
      localVideoSource = videoSource
    }

    // start the flow
    signaling.start()
  }

  /**
   * Leave room and disconnect
   */
  fun leave() {
    signaling.stop()

    peerConnections.keys.forEach { disconnectPeer(it) }

    localMediaStream?.dispose()
    localAudioSource?.dispose()
    localVideoCapturer?.dispose()
    localVideoSource?.dispose()

    localEglBase.release()
    remoteEglBase.release()
    peerConnectionFactory.dispose()
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
    localVideoTrack?.addSink(view)
  }

  /**
   * Render remote video to the renderer
   * Please note that this should be called in main thread only
   */
  fun addRemoteVideoRenderer(peerId: String, renderer: SurfaceViewRenderer) {
    peerVideoTracks[peerId]?.addSink(renderer)
  }

  fun removeRemoteVideoRenderer(peerId: String, renderer: SurfaceViewRenderer) {
    peerVideoTracks[peerId]?.removeSink(renderer)
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

  // data channel

  /**
   * Send message to everyone in the room
   */
  fun sendMessage(message: String) {
    val string = "{\"$DATA_CHANNEL_MESSAGE_KEY\": \"$message\"" +
        if (alias.isNullOrBlank()) "}" else ",\"$DATA_CHANNEL_NAME_KEY\":\"$alias\"}"
    val buffer = ByteBuffer.wrap(string.toByteArray())
    val data = DataChannel.Buffer(buffer, false)
    peerDataChannels.forEach { _, dataChannel -> dataChannel.send(data) }
  }

  // Peer connection

  private fun offer(peerId: String) {
    val peerConnection = peerConnectionFactory.createPeerConnection(emptyList(), PeerObserver(peerId))
    peerConnection?.let {
      it.addStream(localMediaStream)
      it.createOffer(SdpObserver(peerId), MediaConstraints())
      val channel = it.createDataChannel(DATA_CHANNEL_LABEL, DataChannel.Init())
      channel.registerObserver(DataChannelObserver(peerId))
      peerDataChannels[peerId] = channel
      peerConnections[peerId] = it
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

  private fun disconnectPeer(peerId: String) {
    peerDataChannels[peerId]?.dispose()
    peerConnections[peerId]?.let {
      it.removeStream(localMediaStream)
      it.dispose()
    }
    peerDataChannels.remove(peerId)
    peerVideoTracks.remove(peerId)
    peerConnections.remove(peerId)
    callback.onCsioRTCPeerUpdate()
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

    override fun onDataChannel(channel: DataChannel) {
      Log.i(TAG, "onDataChannel")
      channel.registerObserver(DataChannelObserver(peerId))
      peerDataChannels[peerId] = channel
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {}
    override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
    override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
    override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
    override fun onRemoveStream(stream: MediaStream?) {}
    override fun onRenegotiationNeeded() {}
    override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
  }

  // Data channel observer

  private inner class DataChannelObserver(val peerId: String) : DataChannel.Observer {
    override fun onMessage(buffer: DataChannel.Buffer) {
      Log.i(TAG, "receive message from $peerId")
      val bytes = ByteArray(buffer.data.remaining())
      buffer.data.get(bytes)
      val message = String(bytes)
      val json = JSONObject(message)
      callback.onCsioRTCPeerMessage(
          json.optString(DATA_CHANNEL_NAME_KEY) ?: peerId,
          json.getString(DATA_CHANNEL_MESSAGE_KEY))
    }

    override fun onBufferedAmountChange(p0: Long) {}
    override fun onStateChange() {}
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
    disconnectPeer(peerId)
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

  override fun onDisconnect() {
    callback.onCsioRTCDisconnect()
    stopCallstats()
  }

  // Callstats Analytic

  private fun initCallstats(deviceID: String): Callstats {
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

    return Callstats(
        appID,
        localID,
        deviceID,
        jwt)
  }

  private fun startCallstats(room: String) {
    callstats.startSession(room)
  }

  private fun stopCallstats() {
    callstats.stopSession()
  }
}