CallStats for Android
=====================

[![jcenter](https://api.bintray.com/packages/callstats-io/maven/callstats/images/download.svg)](https://bintray.com/callstats-io/maven/callstats/_latestVersion)

[Callstats](https://www.callstats.io/) WebRTC analytic library for Android.

## Getting started
### Gradle dependency

```
implementation "io.callstats:callstats:<version>"
```

Library will requires WebRTC library to be available at runtime.
```
implementation "org.webrtc:google-webrtc:<version>"
```
For more information https://webrtc.org/native-code/android/

### Create Callstats object
```kotlin
callstats = Callstats(
    context,
    appID, // Application ID from Callstats
    localID, // current user ID
    deviceID, // unique device ID
    jwt, // jwt from server for authentication
    alias) // (Optional) user alias
```

### Send events
When starting the call, call `startSession` with room identifier
```kotlin
callstats.startSession(room)
```

These events need to be forwarded to the library in order to start tracking the call. Add followings into your WebRTC `PeerConnection.Observer` For example:
```kotlin
override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
  callstats.reportEvent(peerId, OnIceConnectionChange(state))
}

override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
  callstats.reportEvent(peerId, OnIceGatheringChange(state))
}

override fun onSignalingChange(state: PeerConnection.SignalingState) {
   callstats.reportEvent(peerId, OnSignalingChange(state))
}
```

And when call finished
``` kotlin
callstats.stopSession()
```

You can take a look at how to send more events in demo application.