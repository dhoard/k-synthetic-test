# Configuration examples

----

Filename

- `test.properties`

Configuration scenario

- Basic self-managed Kafka cluster with 3 brokers
- No Kafka authentication
- HTTP BASIC authentication disable
- HTTP SSL server disabled

Usage

```shell
java -jar target/k-synthetic-test-0.0.5.jar configuration/test.properties 
```

---

Filename

- `test.ssl.properties`

Configuration scenario

- Basic self-managed Kafka cluster with 3 brokers
- No Kafka authentication
- HTTP BASIC authentication enabled
- HTTP SSL server enabled

```shell
java \
  -Djavax.net.ssl.keyStore=configuration/keystore.pkcs12 \
  -Djavax.net.ssl.keyStorePassword=changeit \
  -jar target/k-synthetic-test-0.0.5.jar configuration/test.ssl.properties 
```

---

Filename

- `test.confluent-cloud.properties`

Configuration scenario

- Basic shared Confluent Cloud cluster
- Confluent Cloud authentication
- HTTP BASIC authentication enabled
- HTTP SSL server enabled

```shell
java \
  -Djavax.net.ssl.keyStore=configuration/keystore.pkcs12 \
  -Djavax.net.ssl.keyStorePassword=changeit \
  -jar target/k-synthetic-test-0.0.5.jar configuration/test.confluent-cloud.properties 
```