# kafka-synthetic-test

Kafka synthetic test application to measure round trip time.

## Build

```
cd <project directory>
mvn clean package
```

## Run

Create a topic with a partition count that is equal to the number of Kafka brokers.

Copy `test.properties` and edit to match your environment

Execute `./kafka-synthetic-test.sh <test properties>`

Access Prometheus metrics using `http://<http.server.address>:<http.server.port>`

**Notes**

- A test message is sent to every partition based on the configured `period.ms` value


- Because the message only contains the produce time, latency will be greater than a real Kafka use case

# FOR DEMO PURPOSES ONLY - NOT SUPPORTED

