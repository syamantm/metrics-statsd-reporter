package com.syamantakm.metrics

import java.util
import java.util.concurrent.TimeUnit

import com.codahale.metrics._
import com.syamantakm.metrics.tagging.Tagged
import com.timgroup.statsd.StatsDClient
import scala.collection.JavaConverters._
import org.slf4j.LoggerFactory

/**
 *
 * @author syamantak.
 */
class StatsDReporter(name: String,
                     registry: MetricRegistry,
                     rateUnit: TimeUnit,
                     durationUnit: TimeUnit,
                     filter: MetricFilter,
                     statsDClient: StatsDClient,
                     tags: Seq[String] = Seq.empty[String])
  extends ScheduledReporter(registry, name, filter, rateUnit, durationUnit) {

  private val logger = LoggerFactory.getLogger(getClass)

  override def report(gauges: util.SortedMap[String, Gauge[_]],
                      counters: util.SortedMap[String, Counter],
                      histograms: util.SortedMap[String, Histogram],
                      meters: util.SortedMap[String, Meter],
                      timers: util.SortedMap[String, Timer]): Unit = {

    gauges.asScala.foreach { case (key, value) => reportGauge(key, value)}
    counters.asScala.foreach { case (key, value) => reportCounter(key, value)}
    histograms.asScala.foreach { case (key, value) => reportHistogram(key, value)}
    meters.asScala.foreach { case (key, value) => reportMetered(key, value)}
    timers.asScala.foreach { case (key, value) => reportTimer(key, value)}
  }

  protected def reportCounter(key: String, value: Counter): Unit = {
    statsDClient.count(key, value.getCount,eventTags(value):_*)
  }

  protected def reportTimer(key: String, value: Timer): Unit = {
    reportSampling(key, value)
  }

  protected def reportHistogram(key: String, value: Histogram): Unit = {
    reportSampling(key, value)
  }

  protected def reportMetered(key: String, value: Metered): Unit = {
    statsDClient.histogram(named(key, "count"), value.getCount, eventTags(value):_*)
    statsDClient.histogram(named(key, "mean"), value.getMeanRate, eventTags(value):_*)
    statsDClient.histogram(named(key, "m1_rate"), value.getOneMinuteRate, eventTags(value):_*)
    statsDClient.histogram(named(key, "m5_rate"), value.getFiveMinuteRate, eventTags(value):_*)
    statsDClient.histogram(named(key, "m15_rate"), value.getFifteenMinuteRate, eventTags(value):_*)
  }

  protected def reportGauge(name: String, gauge: Gauge[_]): Unit = {
    gauge.getValue match {
      case longValue: Long => statsDClient.recordGaugeValue(name, longValue, eventTags(gauge):_*)
      case doubleValue: Double => statsDClient.recordGaugeValue(name, doubleValue, eventTags(gauge):_*)
      case _ => logger.warn(s"gauge is not supported for this type")
    }
  }

  private def reportSampling(key: String, value: Sampling): Unit = {
    val snapshot = value.getSnapshot
    statsDClient.histogram(named(key, "max"), snapshot.getMax, eventTags(value):_*)
    statsDClient.histogram(named(key, "min"), snapshot.getMin, eventTags(value):_*)
    statsDClient.histogram(named(key, "mean"), snapshot.getMean, eventTags(value):_*)
    statsDClient.histogram(named(key, "stddev"), snapshot.getStdDev, eventTags(value):_*)
    statsDClient.histogram(named(key, "p50"), snapshot.getMedian, eventTags(value):_*)
    statsDClient.histogram(named(key, "p75"), snapshot.get75thPercentile, eventTags(value):_*)
    statsDClient.histogram(named(key, "p95"), snapshot.get95thPercentile, eventTags(value):_*)
    statsDClient.histogram(named(key, "p98"), snapshot.get98thPercentile, eventTags(value):_*)
    statsDClient.histogram(named(key, "p99"), snapshot.get99thPercentile, eventTags(value):_*)
    statsDClient.histogram(named(key, "p999"), snapshot.get999thPercentile, eventTags(value):_*)
  }

  private def named(name: String, suffixes: String*): String = MetricRegistry.name(name, suffixes:_*)

  private def eventTags(value: AnyRef): Seq[String] = {
    var metricTags = tags
    if (value.isInstanceOf[Tagged]) {
      metricTags = value.asInstanceOf[Tagged].tags
    }
    metricTags
  }
}
