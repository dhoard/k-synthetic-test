[![Build](https://github.com/dhoard/k-synthetic-test/actions/workflows/build.yml/badge.svg)](https://github.com/dhoard/k-synthetic-test/actions/workflows/build.yml)
[![Code Grade](https://api.codiga.io/project/35752/status/svg)](https://app.codiga.io/hub/project/35752/k-synthetic-test)
[![Code Quality](https://api.codiga.io/project/35752/score/svg)](https://app.codiga.io/hub/project/35752/k-synthetic-test)

# k-synthetic-test

KSyntheticTest is a synthetic test application to measure Kafka produce/consume round trip times

## Build

```sh
cd <project directory>
mvn clean package
```

## Kafka Topic Configuration

You need to create a unique test topic per application instance with enough partitions to span all brokers

- topic partition count increases are automatically handled
- It may take up to 60000 ms to reflect the new topic partition count

### Self-Managed Kafka

The configuration `test.properties` in https://github.com/dhoard/k-synthetic-test/blob/main/configuration/test.properties should be self-explanatory

### Confluent Cloud

**Step 1**

Confluent Cloud client configuration

1. Login to the Confluent Cloud console
2. Select your environment
3. Select your cluster
4. Using the left menu, select `Java clients`
5. Create a new API key / secret (if required)
6. Copy the properties and merge them into your `test.confluent-cloud.properties`

**Step 2**

Install `kcat` (https://github.com/edenhill/kcat)

**Step 3**

Run `kcat` to get broker information

Example (in a shell):

```shell
export CCLOUD_BROKERS=<BROKER DETAILS>
export CCLOUD_ACCESS_KEY_ID=<CCLOUD-APIKEY>
export CCLOUD_SECRET_ACCESS_KEY=<CCLOUD-APISECRET>

kcat -b ${CCLOUD_BROKERS} -L \
   -X security.protocol=SASL_SSL \
   -X sasl.mechanisms=PLAIN \
   -X sasl.username=${CCLOUD_ACCESS_KEY_ID} \
   -X sasl.password=${CCLOUD_SECRET_ACCESS_KEY} \
```

**Notes**

- This application uses manual partition assignment
  - dynamic Kafka partition increases are currently not handle


- Example topic name is `k-synthetic-test-<id>`
  - where `<id>` matches the `id` in your test properties


- Example retention time is `300,000` ms (5 minutes)
  - old messages provide no value, so are skipped


## Run

**Step 1**

Copy `configuration/test.properties` and edit to match your environment

**Step 2**

Run

```shell
java -jar target/k-synthetic-test-0.0.6.jar configuration/test.properties
```

**NOTES**

- Other configuration examples can be found at https://github.com/dhoard/k-synthetic-test/configuration


- If you enable HTTPS (`http.server.ssl.enabled=true`) you need to provide Java system properties
  - `-Djavax.net.ssl.keyStore`
  - `-Djavax.net.ssl.keyStorePassword`

## Metrics

Access Prometheus metrics using `http://<http.server.address>:<http.server.port>`

Example URL (based on `test.properties`:

```
http://localhost:9191
```

Example output:

```
# HELP k_synthetic_test_round_trip_time Kafka synthetic test round trip time. Negative indicates no update within the configured "metric.expiration.period.ms" period
# TYPE k_synthetic_test_round_trip_time gauge
k_synthetic_test_round_trip_time{id="source-10.0.0.1",bootstrap_servers="cp-1:9092,cp-2:9092,cp-3:9092",topic="k-synthetic-test-source-10.0.0.1",partition="2",} 8.0
k_synthetic_test_round_trip_time{id="source-10.0.0.1",bootstrap_servers="cp-1:9092,cp-2:9092,cp-3:9092",topic="k-synthetic-test-source-10.0.0.1",partition="1",} 9.0
k_synthetic_test_round_trip_time{id="source-10.0.0.1",bootstrap_servers="cp-1:9092,cp-2:9092,cp-3:9092",topic="k-synthetic-test-source-10.0.0.1",partition="0",} 9.0
```

**Notes**

- A test message is sent to every partition based on the configured `period.ms` value


- A negative value indicates that a metric hasn't been updated within the configured `metric.expiration.period.ms` value

# Notices

Apache, Apache Kafka, Kafka, and associated open source project names are trademarks of the Apache Software Foundation

- https://apache.org/
- https://kafka.apache.org/

Confluent and Confluent Cloud are copyrighted Confluent, Inc. 2014-2023

- https://www.confluent.io/
