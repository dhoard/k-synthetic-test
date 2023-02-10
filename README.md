# kafka-synthetic-test

Kafka synthetic test application to measure round trip time.

## Build

```
cd <project directory>
mvn clean package
```

## Run

Create a topic with a partition count that is equal to the number of Kafka brokers.

Edit `producer.properties` to match your Kafka environment.

Edit `consumer.properties` to match your Kafka environment.

Execute `./KafkaSyntheticTest.sh producer.properties consumer.properties`

Access Prometheus metrics using `http://localhost:8181`

**Notes**

- A test message is sent to every partition on a 10-second timer


- Because the message only contains the produce time, latency will be much greater than a real Kafka use case

# FOR DEMO PURPOSES ONLY

