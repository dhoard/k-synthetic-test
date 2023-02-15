# kafka-synthetic-test

Kafka synthetic test application to measure round trip time.

**Because the message only contains the produce timestamp (Unix epoch), latency will be greater than a real Kafka use case**

## Build

```
cd <project directory>
mvn clean package
```

## Kafka Topic Configuration

You need to create a unique test topic per application instance with enough partitions to span all brokers

**Notes**

- This application uses manual partition assignment


- Example topic name is `kafka-synthetic-test-<id>`... where `<id>` matches the `id` in `test.properties`


- Example retention time is `300,000` ms (5 minutes) (Old messages are skipped)


## Run


Copy `test.properties` and edit to match your environment

Execute `./kafka-synthetic-test.sh <test properties>`

## Metrics

Access Prometheus metrics using `http://<http.server.address>:<http.server.port>`

Example URL (based on `test.properties`:

```
http://localhost:9191
```

Example output:

```
# HELP kafka_synthetic_test_round_trip_time Kafka synthetic test round trip time
# TYPE kafka_synthetic_test_round_trip_time gauge
kafka_synthetic_test_round_trip_time{id="source-10.0.0.1",bootstrap_servers="cp-3:9092",topic="kafka-synthetic-test",partition="0",} 6.0
kafka_synthetic_test_round_trip_time{id="source-10.0.0.1",bootstrap_servers="cp-3:9092",topic="kafka-synthetic-test",partition="1",} 7.0
kafka_synthetic_test_round_trip_time{id="source-10.0.0.1",bootstrap_servers="cp-3:9092",topic="kafka-synthetic-test",partition="2",} 9.0
```

**Notes**

- A test message is sent to every partition based on the configured `period.ms` value


- A negative value indicates that a metric hasn't been updated within the configured `metric.expiration.period.ms` value


# FOR DEMO PURPOSES ONLY - NOT SUPPORTED

