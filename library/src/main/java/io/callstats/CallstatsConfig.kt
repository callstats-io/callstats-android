package io.callstats

class CallstatsConfig {

  /** Send keep alive event every x second */
  var keepAlivePeriod = 10

  /** Stats submission period */
  var statsSubmissionPeriod = 30

  /** System stats submission period */
  var systemStatsSubmissionPeriod = 30
}