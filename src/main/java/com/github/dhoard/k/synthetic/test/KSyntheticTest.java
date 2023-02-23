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

package com.github.dhoard.k.synthetic.test;

import com.google.common.net.InetAddresses;
import com.google.common.net.InternetDomainName;
import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpsConfigurator;
import io.prometheus.client.exporter.HTTPServer;
import nl.altindag.ssl.SSLFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * Class to implement a synthetic Kafka performance test
 */
public class KSyntheticTest implements Consumer<ConsumerRecord<String, String>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KSyntheticTest.class);

    private final CountDownLatch countDownLatch;
    private String id;
    private String topic;
    private String bootstrapServers;
    private boolean logResponses;
    private ExpiringGauge roundTripTimeExpiringGauge;

    /**
     * Constructor
     */
    public KSyntheticTest() {
        banner(getClass().getSimpleName() + " " + Information.getVersion());

        countDownLatch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread(countDownLatch::countDown));
    }

    /**
     * Method to run the test application
     *
     * @param filename
     * @throws Exception
     */
    private void run(String filename) throws Exception {
        if ((filename == null) || filename.isBlank()) {
            throw new ConfigurationException("filename argument is required");
        }

        Configuration configuration = new Configuration();
        configuration.load(filename.trim());

        id = configuration.asString("id");
        LOGGER.info(String.format("id [%s]", id));

        long delayMs = configuration.asLong("delay.ms");
        if (delayMs < 0) {
            throw new ConfigurationException("property \"delay.ms\" must be >= 0");
        }
        LOGGER.info(String.format("delay.ms [%d]", delayMs));

        long periodMs = configuration.asLong("period.ms");
        if (periodMs < 0) {
            throw new ConfigurationException("property \"period.ms\" must be >= 0");
        }
        LOGGER.info(String.format("period.ms [%d]", periodMs));

        long metricExpirationPeriodMs = configuration.asLong("metric.expiration.period.ms");
        if (metricExpirationPeriodMs <= 0) {
            throw new ConfigurationException("property \"metric.expiration.period.ms\" must be > 0");
        }
        LOGGER.info(String.format("metric.expiration.period.ms [%s]", metricExpirationPeriodMs));

        logResponses = configuration.asBoolean("log.responses", false);
        LOGGER.info(String.format("log.responses [%b]", logResponses));

        String httpServerAddress = configuration.asString("http.server.address");
        if (!InetAddresses.isUriInetAddress(httpServerAddress) && !InternetDomainName.isValid(httpServerAddress)) {
            throw new ConfigurationException("property \"http.server.address\" doesn't appear to be an IP address or host name");
        }
        LOGGER.info(String.format("http.server.address [%s]", httpServerAddress));

        int httpServerPort = configuration.asInt("http.server.port");
        if (httpServerPort < 1 || httpServerPort > 65535) {
            throw new ConfigurationException("property \"http.server.port\" must be >= 1 and <= 65535");
        }
        LOGGER.info(String.format("http.server.port [%d]", httpServerPort));

        HTTPServer.Builder httpServerBuilder = new HTTPServer.Builder();
        httpServerBuilder
                .withDaemonThreads(true)
                .withHostname(httpServerAddress)
                .withPort(httpServerPort);

        boolean httpServerBasicAuthenticationEnabled = configuration.asBoolean("http.server.basic.authentication.enabled", false);
        LOGGER.info(String.format("http.server.basic.authentication.enabled [%b]", httpServerBasicAuthenticationEnabled));

        if (httpServerBasicAuthenticationEnabled) {
            final String httpServerBasicAuthenticationUsername = configuration.asString("http.server.basic.authentication.username");
            final String httpServerBasicAuthenticationPassword = configuration.asString("http.server.basic.authentication.password");

            LOGGER.info(String.format("http.server.basic.authentication.username [%s]", httpServerBasicAuthenticationUsername));
            LOGGER.info("http.server.basic.authentication.password [*] (masked)");

            httpServerBuilder.withAuthenticator(new BasicAuthenticator("/") {
                @Override
                public boolean checkCredentials(String username, String password) {
                    return httpServerBasicAuthenticationUsername.equals(username)
                            && httpServerBasicAuthenticationPassword.equals(password);
                }
            });
        }

        boolean httpServerSslEnabled = configuration.asBoolean("http.server.ssl.enabled", false);
        LOGGER.info(String.format("http.server.ssl.enabled [%b]", httpServerSslEnabled));

        if (httpServerSslEnabled) {
            String certificateAlias = configuration.asString("http.server.ssl.certificate.alias");
            LOGGER.info(String.format("http.server.ssl.certificate.alias [%s]", certificateAlias));

            SSLFactory sslFactory =
                    SSLFactory.builder()
                            .withSystemPropertyDerivedIdentityMaterial()
                            .withIdentityRoute(certificateAlias, "https://" + httpServerAddress + ":" + httpServerPort)
                            .build();

            SSLContext sslContext = sslFactory.getSslContext();

            httpServerBuilder.withHttpsConfigurator(new HttpsConfigurator(sslContext));
        }

        bootstrapServers = configuration.asString("bootstrap.servers");
        LOGGER.info(String.format("bootstrap.servers [%s]", bootstrapServers));

        topic = configuration.asString("topic");
        LOGGER.info(String.format("topic [%s]", topic));

        roundTripTimeExpiringGauge = new ExpiringGauge.Builder()
                .name("k_synthetic_test_round_trip_time")
                .help("Kafka synthetic test round trip time. Negative indicates no update within the configured \"metric.expiration.period.ms\" period")
                .labelNames("id", "bootstrap_servers", "topic", "partition")
                .ttl(metricExpirationPeriodMs)
                .register();

        HTTPServer httpServer = httpServerBuilder.build();

        // Remove general test properties

        configuration.remove("id");
        configuration.remove("delay.ms");
        configuration.remove("period.ms");
        configuration.remove("metric.expiration.period.ms");
        configuration.remove("log.responses");
        configuration.remove("http.server.address");
        configuration.remove("http.server.port");
        configuration.remove("http.server.basic.authentication.enabled");
        configuration.remove("http.server.basic.authentication.username");
        configuration.remove("http.server.basic.authentication.password");
        configuration.remove("http.server.ssl.enabled");
        configuration.remove("http.server.ssl.certificate.alias");

        // Create specific producer and consumer configuration with a subset of properties
        // to prevent "These configurations X were supplied but are not used yet" warnings

        Configuration recordConsumerConfiguration = configuration.copy();
        recordConsumerConfiguration.put("metadata.max.age.ms", "60000");
        recordConsumerConfiguration.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        recordConsumerConfiguration.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        recordConsumerConfiguration.remove("acks");
        recordConsumerConfiguration.remove("linger.ms");
        recordConsumerConfiguration.remove("key.serializer");
        recordConsumerConfiguration.remove("value.serializer");

        RecordConsumer recordConsumer = new RecordConsumer(recordConsumerConfiguration, this);
        recordConsumer.start();

        Configuration recordProducerConfiguration = configuration.copy();
        recordProducerConfiguration.put("metadata.max.age.ms", "60000");
        recordProducerConfiguration.remove("key.deserializer");
        recordProducerConfiguration.remove("value.deserializer");
        recordProducerConfiguration.remove("session.timeout.ms");
        recordProducerConfiguration.put("batch.size", "0");
        recordProducerConfiguration.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        recordProducerConfiguration.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        if (!recordProducerConfiguration.containsKey("acks")) {
            recordProducerConfiguration.put("acks", "all");
        }

        if (!recordProducerConfiguration.containsKey("linger.ms")) {
            recordProducerConfiguration.put("linger.ms", "0");
        }

        RecordProducer recordProducer = new RecordProducer(id, delayMs, periodMs, recordProducerConfiguration);
        recordProducer.start();

        LOGGER.info("running");

        countDownLatch.await();

        httpServer.close();
        recordProducer.close();
        recordConsumer.close();
    }

    /**
     * Method to accept a ConsumerRecord
     *
     * @param consumerRecord
     */
    public void accept(ConsumerRecord<String, String> consumerRecord) {
        Header[] headers = consumerRecord.headers().toArray();
        for (Header header : headers) {
            if ("id".equals(header.key())) {
                String value = new String(header.value(), StandardCharsets.UTF_8);
                if (id.equals(value)) {
                    process(consumerRecord);
                }

                break;
            }
        }
    }

    /**
     * Method to process a ConsumerRecord
     *
     * @param consumerRecord
     */
    private void process(ConsumerRecord<String, String> consumerRecord) {
        long recordValueTimestampMs = Long.parseLong(consumerRecord.value());
        long nowMs = System.currentTimeMillis();
        long elapsedTimeMs = nowMs - recordValueTimestampMs;
        String partition = String.valueOf(consumerRecord.partition());

        roundTripTimeExpiringGauge
                .labels(
                        id,
                        bootstrapServers,
                        topic,
                        partition)
                .set(elapsedTimeMs);

        if (logResponses) {
            LOGGER.info(
                    String.format(
                            "id [%s] bootstrap.servers [%s] topic [%s] partition [%d] round trip time [%d] ms",
                            id,
                            bootstrapServers,
                            topic,
                            consumerRecord.partition(), elapsedTimeMs));
        }
    }

    /**
     * Main method
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        if ((args == null) || (args.length != 1)) {
            System.out.println("Usage: java -jar <jar> <properties>");
            return;
        }

        try {
            new KSyntheticTest().run(args[0]);
        } catch (ConfigurationException e) {
            LOGGER.error(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Method to print the application banner
     *
     * @param string
     */
    private void banner(String string) {
        String line = String.format("%0" + string.length() + "d", 0).replace('0', '-');
        LOGGER.info(line);
        LOGGER.info(string);
        LOGGER.info(line);
    }
}
