# metrics-statsd-reporter

A MetricsRegistry with tagging support and a StatsDReporter for datadog. 

## Features

* Tagging support with Metric
* Histogram support
* Datadog(StatsD) reporter using [Non blocking StatsD client](https://github.com/indeedeng/java-dogstatsd-client)

## Usage

Create a new [TaggedMetrics](src/main/scala/com/syamantakm/metrics/tagging/TaggedMetrics.scala)

```scala
import com.codahale.metrics.MetricRegistry
import com.syamantakm.metrics.TaggedMetrics
...
val metricRegistry = new MetricRegistry()
val taggedMetrics = TaggedMetrics(metricRegistry)
```
 
Now use this [TaggedMetrics](src/main/scala/com/syamantakm/metrics/tagging/TaggedMetrics.scala) to record metrics in your code.
 
### Counter
 
```scala
 val successCounter = taggedMetrics.counter("test.counter", "success", "host1") // "success", "host1" are tags
 // do stuff
 successCounter.inc(delta = 3)
```

### Gauge

```scala
//record int gauge
taggedMetrics.gaugeInt("test.gaugeInt", "success", "host1")  {
  // do something that returns an Int
  // e.g. getQueueSize
}
....
//record long gauge
taggedMetrics.gaugeLong("test.gaugeLong", "success", "host1") {
  // do something that returns a Long
  // e.g. getRowCount
}
...
//record double gauge
taggedMetrics.gaugeDouble("test.gaugeDouble", "success", "host1") {
  // do something that returns a Double
  // e.g. getErrorRate
}
```

### Timer

```scala
val timer = taggedMetrics.timer("test.executionTime", "success", "host1")
// execute something
timer.time {
  println("Start execution")
  TimeUnit.MILLISECONDS.sleep(10)
  println("End execution")
}
```

### Send reports to Datadog (via StatsD)

```scala
import com.codahale.metrics.{MetricFilter, MetricRegistry}
import com.syamantakm.metrics.StatsDReporter
import com.timgroup.statsd.NonBlockingStatsDClient
...
val metricRegistry = new MetricRegistry()
.....
//use metricRegistry in code
// start a reporter
val statsDClient = new NonBlockingStatsDClient("my.app", "localhost", 8098)
val reporter = StatsDReporter(
                  name = "my-statsd-reporter",
                  registry = metricRegistry,
                  statsDClient = statsDClient
                )
reporter.start(1, TimeUnit.SECONDS)
```
 
 