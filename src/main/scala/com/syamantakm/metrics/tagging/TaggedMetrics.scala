package com.syamantakm.metrics.tagging

import com.codahale.metrics.{Metric, Gauge => DWGauge, Counter => DWCounter, Histogram => DWHistogram, Meter => DWMeter, Timer => DWTimer, _}
import nl.grons.metrics.scala._

import scala.collection.concurrent.TrieMap

/**
 * @author syamantak.
 */

case class TaggedMetricName(name: String, tags: Seq[String])

class TaggedMetrics(delegate: MetricRegistry) extends InstrumentedBuilder {
  private val taggedCounters = new TrieMap[TaggedMetricName, DWCounter]
  private val taggedHistogram = new TrieMap[TaggedMetricName, DWHistogram]
  private val taggedMeters = new TrieMap[TaggedMetricName, DWMeter]
  private val taggedTimer = new TrieMap[TaggedMetricName, DWTimer]
  private val taggedIntGauge = new TrieMap[TaggedMetricName, DWGauge[Int]]
  private val taggedLongGauge = new TrieMap[TaggedMetricName, DWGauge[Long]]
  private val taggedDoubleGauge = new TrieMap[TaggedMetricName, DWGauge[Double]]

  override val metricRegistry: MetricRegistry = delegate

  def gaugeInt(name: String, tags: String*)(f: => Int): Gauge[Int] = {
    gauge[Int](name, taggedIntGauge, tags)(f)
  }

  def gaugeLong(name: String, tags: Seq[String])(f: => Long): Gauge[Long] = {
    gauge[Long](name, taggedLongGauge, tags)(f)
  }

  def gaugeDouble(name: String, tags: Seq[String])(f: => Double): Gauge[Double] = {
    gauge[Double](name, taggedDoubleGauge, tags)(f)
  }
  
  def counter(name: String, tags: String*): Counter = {
    val key = TaggedMetricName(name, tags)
    new Counter(registerWithTags[DWCounter](taggedCounters, key, create => {
      new DWCounter with Tagged {
        override def tags: Seq[String] = key.tags
      }
    }))
  }
  
  def histogram(name: String, tags: String*): Histogram = {
    val key = TaggedMetricName(name, tags)
    new Histogram(registerWithTags[DWHistogram](taggedHistogram, key, create => {
      new DWHistogram(new ExponentiallyDecayingReservoir) with Tagged {
        override def tags: Seq[String] = key.tags
      }
    }))
  }

  def meter(name: String, tags: String*): Meter = {
    val key = TaggedMetricName(name, tags)
    new Meter(registerWithTags[DWMeter](taggedMeters, key, create => {
      new DWMeter with Tagged {
        override def tags: Seq[String] = key.tags
      }
    }))
  }

  def timer(name: String, tags: String*): Timer = {
    val key = TaggedMetricName(name, tags)
    new Timer(registerWithTags[DWTimer](taggedTimer, key, create => {
      new DWTimer with Tagged {
        override def tags: Seq[String] = key.tags
      }
    }))
  }

  private def gauge[T](name: String,
                       map: TrieMap[TaggedMetricName, DWGauge[T]],
                       tags: Seq[String])(f: => T)(implicit m: Manifest[T]): Gauge[T] = {
    val key = TaggedMetricName(name, tags)
    new Gauge[T](registerWithTags[DWGauge[T]](map, key, create =>
    {new DWGauge[T] with Tagged {
      override def tags: Seq[String] = key.tags
      override def getValue: T = f
    }}))
  }
  
  private def registerWithTags[T <: Metric](map: TrieMap[TaggedMetricName, T],
                                             key: TaggedMetricName,
                                             f: (TaggedMetricName) => T)(implicit m: Manifest[T]): T = {
    map.get(key) match {
      case Some(value) => value
      case None =>
        val newMetrics = f(key)
        val existing = map.putIfAbsent(key, newMetrics)
        existing match {
          case Some(old) => old
          case None =>
            metricRegistry.register(mkName(key.name, key.tags), newMetrics)
            newMetrics
        }
    }
  }

  private def mkName(name: String, tags: Seq[String]) = s"$name.${tags.mkString(".")}"
}
