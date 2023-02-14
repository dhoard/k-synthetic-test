/*
 * Copyright 2023 Douglas Hoard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dhoard.kafka;

import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.Reader;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

public class KafkaSyntheticTest implements Consumer<ConsumerRecords<String, String>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaSyntheticTest.class);

    public static void main(String[] args) throws Exception {
        if ((args == null) || (args.length != 1)) {
            System.out.println("Usage: java -jar <jar> <properties>");
            return;
        }

        try {
            new KafkaSyntheticTest().run(args);
        } catch (ConfigurationException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private final CountDownLatch countDownLatch;
    private String id;
    private String topic;
    private String bootstrapServers;
    private String groupId;
    private String httpServerAddress;
    private int httpServerPort;
    private boolean logResponses;
    private MessageProducer messageProducer;
    private MessageConsumer messageConsumer;
    private ExpiringGauge roundTripTimeExpiringGauge;
    private HTTPServer httpServer;

    public KafkaSyntheticTest() {
        LOGGER.info(getClass().getSimpleName() + " v" + Information.getVersion());

        countDownLatch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("shutdown");
            countDownLatch.countDown();
        }));
    }

    public void run(String[] args) throws Exception {
        Properties properties = new Properties();
        try (Reader reader = new FileReader(args[0])) {
            properties.load(reader);
        }

        Configuration configuration = new Configuration(properties);

        id = configuration.asString("id");
        LOGGER.info(String.format("id [%s]", id));

        long delayMs = configuration.asLong("delay.ms", 0L);
        LOGGER.info(String.format("delay.ms [%d]", delayMs));

        long periodMs = configuration.asLong("period.ms", 10000L);
        LOGGER.info(String.format("period.ms [%d]", periodMs));

        long metricExpirationPeriodMs = configuration.asLong("metric.expiration.period.ms");
        LOGGER.info(String.format("metric.expiration.period.ms [%s]", metricExpirationPeriodMs));

        httpServerAddress = configuration.asString("http.server.address");
        LOGGER.info(String.format("http.server.address [%s]", httpServerAddress));

        httpServerPort = configuration.asInt("http.server.port");
        LOGGER.info(String.format("http.server.port [%d]", httpServerPort));

        if (properties.containsKey("log.responses")) {
            String value = configuration.asString("log.responses");

            logResponses =
                    "true".equalsIgnoreCase(value)
                    || "t".equalsIgnoreCase(value);
        }

        LOGGER.info(String.format("log.responses [%b]", logResponses));

        bootstrapServers = configuration.asString("bootstrap.servers");
        LOGGER.info(String.format("bootstrap.servers [%s]", bootstrapServers));

        topic = configuration.asString("topic");
        LOGGER.info(String.format("topic [%s]", topic));

        roundTripTimeExpiringGauge = new ExpiringGauge.Builder()
                .name("kafka_synthetic_test_round_trip_time")
                .help("Kafka synthetic test round trip time. Negative indicates no update within \"metric.expiration.period.ms\"")
                .labelNames("id", "bootstrap.servers".replace(".", "_"), "topic", "partition")
                .ttl(metricExpirationPeriodMs)
                .register();

        httpServer = new HTTPServer.Builder()
                .withDaemonThreads(true)
                .withHostname(httpServerAddress)
                .withPort(httpServerPort)
                .build();

        // Remove test properties

        properties.remove("id");
        properties.remove("topic");
        properties.remove("delay.ms");
        properties.remove("period.ms");
        properties.remove("metric.expiration.period.ms");
        properties.remove("http.server.address");
        properties.remove("http.server.port");
        properties.remove("log.responses");

        // Create specific producer and consumer properties with a subset of properties
        // to prevent "These configurations X were supplied but are not used yet" warnings

        Properties consumerProperties = new Properties();
        consumerProperties.putAll(properties);
        consumerProperties.remove("acks");
        consumerProperties.remove("linger.ms");
        consumerProperties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProperties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProperties.remove("key.serializer");
        consumerProperties.remove("value.serializer");

        messageConsumer = new MessageConsumer(consumerProperties, topic, periodMs,this);

        Properties producerProperties = new Properties();

        producerProperties.putAll(properties);
        producerProperties.put("batch.size", "0");
        producerProperties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProperties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        if (!producerProperties.containsKey("acks")) {
            producerProperties.put("acks", "all");
        }

        if (!producerProperties.containsKey("linger.ms")) {
            producerProperties.put("linger.ms", "0");
        }

        producerProperties.remove("key.deserializer");
        producerProperties.remove("value.deserializer");
        producerProperties.remove("session.timeout.ms");

        messageProducer = new MessageProducer(producerProperties, topic, delayMs, periodMs);

        countDownLatch.await();

        httpServer.close();
        messageProducer.close();
        messageConsumer.close();
    }

    public void accept(ConsumerRecords<String, String> consumerRecords) {
        for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
            long messageTimestamp = Long.parseLong(consumerRecord.value());
            long now = System.currentTimeMillis();
            long elapsedTime = now - messageTimestamp;
            String partition = String.valueOf(consumerRecord.partition());

            roundTripTimeExpiringGauge
                    .labels(
                            id,
                            bootstrapServers,
                            topic,
                            partition)
                    .set(elapsedTime);

            if (logResponses) {
                LOGGER.info(
                        String.format(
                                "id [%s] bootstrap.servers [%s] topic [%s] partition [%d] round trip time [%d] ms",
                                id,
                                bootstrapServers,
                                topic,
                                consumerRecord.partition(), elapsedTime));
            }
        }
    }
}
