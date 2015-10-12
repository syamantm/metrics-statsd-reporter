package com.syamantakm.metrics

import java.util.concurrent.TimeUnit

import com.codahale.metrics.{MetricFilter, MetricRegistry}
import com.syamantakm.TestUdpServer
import com.syamantakm.metrics.tagging.TaggedMetrics
import com.timgroup.statsd.NonBlockingStatsDClient
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable.{Set => MutableSet, TreeSet}

/**
 * @author syamantak.
 */
@RunWith(classOf[JUnitRunner])
class StatsDReporterTest extends FlatSpec with Matchers {

  val port = 8098

  "StatsDReporter" should "report counters from metric registry" in {
    // Given
    val registry = new MetricRegistry()

    val taggedMetrics = TaggedMetrics(registry)

    val resultCapture = TreeSet[String]()

    val statsDServer = TestUdpServer(port, resultCapture)

    val successCounter = taggedMetrics.counter("test.counter", "success", "host1")
    val failureCounter = taggedMetrics.counter("test.counter", "failure", "host1")

    val client = new NonBlockingStatsDClient("my.app1", "localhost", port)

    val reporter: StatsDReporter = createStatsDReporter(registry, client)

    // When
    successCounter.inc(delta = 3)
    failureCounter.inc()
    reporter.start(1, TimeUnit.SECONDS)
    TimeUnit.SECONDS.sleep(3)

    // Then
    println("*** results captured ***")
    resultCapture.foreach(println)
    println("*** ## ***")
    resultCapture.isEmpty should be(false)

    reporter.stop()
    statsDServer.shutdown()
  }

  it should "report gauges from metric registry" in {
    // Given
    val registry = new MetricRegistry()

    val taggedMetrics = TaggedMetrics(registry)

    val resultCapture = TreeSet[String]()

    val statsDServer = TestUdpServer(port, resultCapture)

    val client = new NonBlockingStatsDClient("my.app1", "localhost", port)

    val reporter: StatsDReporter = createStatsDReporter(registry, client)

    // When
    taggedMetrics.gaugeInt("test.gaugeInt", "success", "host1") {
      getIntGauge
    }

    taggedMetrics.gaugeLong("test.gaugeLong", "success", "host1") {
      getLongGauge
    }

    taggedMetrics.gaugeDouble("test.gaugeDouble", "success", "host1") {
      getDoubleGauge
    }

    reporter.start(1, TimeUnit.SECONDS)
    TimeUnit.SECONDS.sleep(3)

    // Then
    println("*** results captured ***")
    resultCapture.foreach(println)
    println("*** ## ***")
    resultCapture.isEmpty should be(false)

    reporter.stop()
    statsDServer.shutdown()
  }

  it should "report execution time from metric registry" in {
    // Given
    val registry = new MetricRegistry()

    val taggedMetrics = new TaggedMetrics(registry)

    val resultCapture = TreeSet[String]()

    val statsDServer = TestUdpServer(port, resultCapture)

    val timer = taggedMetrics.timer("test.executionTime", "success", "host1")

    val client = new NonBlockingStatsDClient("my.app2", "localhost", port)

    val reporter: StatsDReporter = createStatsDReporter(registry, client)

    // When
    timer.time {
      println("Start execution")
      TimeUnit.MILLISECONDS.sleep(10)
      println("End execution")
    }

    reporter.start(1, TimeUnit.SECONDS)
    TimeUnit.SECONDS.sleep(3)

    // Then
    println("*** results captured ***")
    resultCapture.foreach(println)
    println("*** ## ***")
    resultCapture.isEmpty should be(false)

    reporter.stop()
    statsDServer.shutdown()
  }

  def getIntGauge:Int = 2
  def getLongGauge: Long = 2
  def getDoubleGauge: Double = 2.2

  def createStatsDReporter(registry: MetricRegistry, client: NonBlockingStatsDClient): StatsDReporter = {
    StatsDReporter(
      name = "my-statsd-reporter",
      registry = registry,
      statsDClient = client
    )
  }
}
