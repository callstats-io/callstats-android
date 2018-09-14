package io.callstats.event.info

/**
 * Feedback info
 */
internal data class Feedback(val overallRating: Int) {

  /**
   * It is provided by the developer.
   * Non-empty remoteID means that the feedback was given explicitly about the connection between these two parties.
   * Otherwise it is regarded as general conference feedback.
   */
  var remoteID: String? = null

  /**
   * Rating from 1 to 5
   */
  var videoQualityRating: Int? = null

  /**
   * Rating from 1 to 5
   */
  var audioQualityRating: Int? = null

  /**
   * Comments from the participant
   */
  var comments: String? = null
}