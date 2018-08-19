package io.callstats.event.special

import io.callstats.event.SessionEvent

/**
 * PRO feature: Whenever there is an updated SDP or a pair of local and remote SDPs, this can be sent to callstats.io.
 *
 * @param remoteID remote user identifier
 * @param connectionID Unique identifier of connection between two endpoints.
 */
internal class SdpEvent(
    val remoteID: String,
    val connectionID: String) : SessionEvent() {

  /**
   * Stringified SDP of the local user
   */
  var localSDP: String? = null

  /**
   * Stringified SDP of the remote user
   */
  var remoteSDP: String? = null

  override fun path() = "events/sdp"
}