# kafka-synthetic-test

Kafka synthetic test application to measure round trip time.

## Build

```
cd <project director>
mvn clean package
```

## Run

Edit `producer.properties` to match your Kafka environment

Edit `consumer.properties` to match your Kafka environment

Execute `./KafkaSyntheticTest.sh producer.properties consumer.properties`

Access Prometheus metrics using `http://localhost:8181`

# FOR DEMO PURPOSES ONLY

