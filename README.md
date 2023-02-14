# kafka-synthetic-test

Kafka synthetic test application to measure round trip time.

## Build

```
cd <project directory>
mvn clean package
```

## Run

For **each** instance of the application, create topic with a partition count that is equal to the number of Kafka brokers.

- Suggested name is `kafka-synthetic-test-<id>`... where `<id>` matches the `id` in `test.properties`


Copy `test.properties` and edit to match your environment

Execute `./kafka-synthetic-test.sh <test properties>`

Access Prometheus metrics using `http://<http.server.address>:<http.server.port>`

Example output:

```
# HELP kafka_synthetic_test_round_trip_time Kafka synthetic test round trip time
# TYPE kafka_synthetic_test_round_trip_time gauge
kafka_synthetic_test_round_trip_time{id="us-west-1.32",bootstrap_servers="cp-3:9092",topic="kafka-synthetic-test",partition="2",} 24.0
kafka_synthetic_test_round_trip_time{id="us-west-1.32",bootstrap_servers="cp-3:9092",topic="kafka-synthetic-test",partition="1",} 10.0
kafka_synthetic_test_round_trip_time{id="us-west-1.32",bootstrap_servers="cp-3:9092",topic="kafka-synthetic-test",partition="0",} 16.0
```

**Notes**

- **This application uses manual partition assignment. You need to create a test topic per application instance**


- A test message is sent to every partition based on the configured `period.ms` value


- A negative value indicates that a metric hasn't been updated within the configured `metric.expiration.period.ms` value


- Because the message only contains the produce time, latency will be greater than a real Kafka use case

# FOR DEMO PURPOSES ONLY - NOT SUPPORTED

