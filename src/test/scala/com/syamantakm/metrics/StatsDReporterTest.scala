package com.syamantakm.metrics

import java.util.concurrent.TimeUnit

import scala.collection.mutable
import scala.collection.mutable.{Set => MutableSet, TreeSet}
import com.codahale.metrics.{MetricFilter, MetricRegistry}
import com.syamantakm.TestUdpServer
import com.syamantakm.metrics.tagging.TaggedMetrics
import com.timgroup.statsd.NonBlockingStatsDClient
import org.scalatest.{FlatSpec, Matchers}

/**
 * @author syamantak.
 */
class StatsDReporterTest extends FlatSpec with Matchers {

  val port = 8098

  "StatsDReporter" should "report counters from metric registry" in {
    // Given
    val registry = new MetricRegistry()

    val taggedMetrics = new TaggedMetrics(registry)

    val resultCapture = TreeSet[String]()

    val statsDServer = TestUdpServer(port, resultCapture)

    val successCounter = taggedMetrics.counter("test.counter", "success", "host1")
    val failureCounter = taggedMetrics.counter("test.counter", "failure", "host1")

    val client = new NonBlockingStatsDClient("my.app", "localhost", port)

    val reporter: StatsDReporter = createStatsDReporter(registry, client)

    // When
    successCounter.inc(delta = 3)
    failureCounter.inc()
    reporter.start(1, TimeUnit.SECONDS)
    TimeUnit.SECONDS.sleep(2)

    // Then
    resultCapture.contains("my.app.test.counter:3|c|#host1,success") should be(true)
    resultCapture.contains("my.app.test.counter:1|c|#host1,failure") should be(true)

    statsDServer.shutdown()
  }

  it should "report execution time from metric registry" in {
    // Given
    val registry = new MetricRegistry()

    val taggedMetrics = new TaggedMetrics(registry)

    val resultCapture = TreeSet[String]()

    val statsDServer = TestUdpServer(port, resultCapture)

    val timer = taggedMetrics.timer("test.executionTime", "success", "host1")

    val client = new NonBlockingStatsDClient("my.app", "localhost", port)

    val reporter: StatsDReporter = createStatsDReporter(registry, client)

    // When
    timer.time {
      println("Start execution")
      TimeUnit.MILLISECONDS.sleep(10)
      println("End execution")
    }

    reporter.start(1, TimeUnit.SECONDS)
    TimeUnit.SECONDS.sleep(2)

    // Then
    val Pattern = "my\\.app\\.test\\.executionTime\\.max:\\d+\\|h\\|#host1,success".r
    resultCapture.isEmpty should be(false)

    resultCapture foreach { elem =>
      elem match {
        case Pattern(c) => println("found max")
        case noMatch => println(s"no match $noMatch")
      }
    }

    statsDServer.shutdown()
  }

  def createStatsDReporter(registry: MetricRegistry, client: NonBlockingStatsDClient): StatsDReporter = {
    val reporter = new StatsDReporter(
      name = "my-statsd-reporter",
      registry = registry,
      rateUnit = TimeUnit.MILLISECONDS,
      durationUnit = TimeUnit.MILLISECONDS,
      filter = MetricFilter.ALL,
      statsDClient = client
    )
    reporter
  }
}
