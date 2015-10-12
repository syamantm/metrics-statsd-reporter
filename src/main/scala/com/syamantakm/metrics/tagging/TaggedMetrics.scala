package com.syamantakm.metrics.tagging

import com.codahale.metrics.{Metric, Gauge => DWGauge, Counter => DWCounter, Histogram => DWHistogram, Meter => DWMeter, Timer => DWTimer, _}
import nl.grons.metrics.scala._

import scala.collection.concurrent.TrieMap

/**
 * @author syamantak.
 */

case class TaggedKey(name: String, tags: Seq[String]) {
  def taggedName: String = s"$name.${tags.mkString(".")}"
}

trait TaggedName {
  def name: String
  def tags: Seq[String]
}

class TaggedMetrics(delegate: MetricRegistry) extends InstrumentedBuilder {
  private val taggedCounters = new TrieMap[TaggedKey, DWCounter]
  private val taggedHistogram = new TrieMap[TaggedKey, DWHistogram]
  private val taggedMeters = new TrieMap[TaggedKey, DWMeter]
  private val taggedTimer = new TrieMap[TaggedKey, DWTimer]
  private val taggedIntGauge = new TrieMap[TaggedKey, DWGauge[Int]]
  private val taggedLongGauge = new TrieMap[TaggedKey, DWGauge[Long]]
  private val taggedDoubleGauge = new TrieMap[TaggedKey, DWGauge[Double]]

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
  
  def counter(metricName: String, tags: String*): Counter = {
    val key = TaggedKey(metricName, tags)
    new Counter(registerWithTags[DWCounter](taggedCounters, key, create => {
      new DWCounter with TaggedName {
        override def name: String = metricName
        override def tags: Seq[String] = key.tags
      }
    }))
  }
  
  def histogram(metricName: String, tags: String*): Histogram = {
    val key = TaggedKey(metricName, tags)
    new Histogram(registerWithTags[DWHistogram](taggedHistogram, key, create => {
      new DWHistogram(new ExponentiallyDecayingReservoir) with TaggedName {
        override def name: String = metricName
        override def tags: Seq[String] = key.tags
      }
    }))
  }

  def meter(metricName: String, tags: String*): Meter = {
    val key = TaggedKey(metricName, tags)
    new Meter(registerWithTags[DWMeter](taggedMeters, key, create => {
      new DWMeter with TaggedName {
        override def name: String = metricName
        override def tags: Seq[String] = key.tags
      }
    }))
  }

  def timer(metricName: String, tags: String*): Timer = {
    val key = TaggedKey(metricName, tags)
    new Timer(registerWithTags[DWTimer](taggedTimer, key, create => {
      new DWTimer with TaggedName {
        override def name: String = metricName
        override def tags: Seq[String] = key.tags
      }
    }))
  }

  private def gauge[T](metricName: String,
                       map: TrieMap[TaggedKey, DWGauge[T]],
                       tags: Seq[String])(f: => T)(implicit m: Manifest[T]): Gauge[T] = {
    val key = TaggedKey(metricName, tags)
    new Gauge[T](registerWithTags[DWGauge[T]](map, key, create =>
    {new DWGauge[T] with TaggedName {
      override def name: String = metricName
      override def tags: Seq[String] = key.tags
      override def getValue: T = f
    }}))
  }
  
  private def registerWithTags[T <: Metric](map: TrieMap[TaggedKey, T],
                                             key: TaggedKey,
                                             f: (TaggedKey) => T)(implicit m: Manifest[T]): T = {
    map.get(key) match {
      case Some(value) => value
      case None =>
        val newMetrics = f(key)
        val existing = map.putIfAbsent(key, newMetrics)
        existing match {
          case Some(old) => old
          case None =>
            metricRegistry.register(key.taggedName, newMetrics)
            newMetrics
        }
    }
  }
}
