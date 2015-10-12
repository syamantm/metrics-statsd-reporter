# metrics-statsd-reporter

A MetricsRegistry with tagging support and a StatsDReporter for datadog. 

## Features

* Tagging support with Metric
* Datadog(StatsD) reporter

## Usage

Create a new [TaggedMetrics](src/main/scala/com/syamantakm/metrics/tagging/TaggedMetrics.scala)

```scala
 import com.codahale.metrics.MetricRegistry
 import com.syamantakm.metrics.TaggedMetrics
 ...
 val metricRegistry = new MetricRegistry()
 val taggedMetrics = new TaggedMetrics(metricRegistry)
```
 
Now use this [TaggedMetrics](src/main/scala/com/syamantakm/metrics/tagging/TaggedMetrics.scala) to record metrics in your code.
 
### Counter
 
```scala
 val successCounter = taggedMetrics.counter("test.counter", "success", "host1") // "success", "host1" are tags
 // do stuff
 successCounter.inc(delta = 3)
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
 val client = new NonBlockingStatsDClient("my.app", "localhost", 8098)
 val reporter: StatsDReporter = new StatsDReporter(
                                      name = "my-statsd-reporter",
                                      registry = registry,
                                      rateUnit = TimeUnit.MILLISECONDS,
                                      durationUnit = TimeUnit.MILLISECONDS,
                                      filter = MetricFilter.ALL,
                                      statsDClient = client
                                    )
 reporter.start(1, TimeUnit.SECONDS)
```
 
 