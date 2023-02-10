package com.github.dhoard.kafka;

import io.prometheus.client.Collector;
import io.prometheus.client.Gauge;
import io.prometheus.client.GaugeMetricFamily;
import io.prometheus.client.exporter.HTTPServer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.config.ConfigDef;

import java.io.FileReader;
import java.io.Reader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

public class KafkaSyntheticTest {

    private static final Random RANDOM = new Random();

    private String topic;

    public static void main(String[] args) throws Exception {
        new KafkaSyntheticTest().run(args);
    }

    private Timer timer;
    private KafkaProducerTimerTask kafkaProducerTimerTask;
    private KafkaProducer<String, String> kafkaProducer;
    private KafkaConsumer<String, String> kafkaConsumer;

    private HTTPServer httpServer;

    public void run(String[] args) throws Exception {
        if ((args == null) || (args.length != 2)) {
            System.out.println("Usage: java -jar <jar> <producer properties> <consume properties");
            return;
        }

        Properties producerProperties = new Properties();
        try (Reader reader = new FileReader(args[0])) {
            producerProperties.load(reader);
        }

        Properties consumerProperties = new Properties();
        try (Reader reader = new FileReader(args[1])) {
            consumerProperties.load(reader);
        }

        topic = producerProperties.getProperty("topic");
        producerProperties.remove("topic");

        consumerProperties.remove("topic");

        kafkaProducer = new KafkaProducer<>(producerProperties);
        kafkaConsumer = new KafkaConsumer<>(consumerProperties);

        kafkaProducerTimerTask = new KafkaProducerTimerTask(topic, kafkaProducer);

        timer = new Timer();
        timer.schedule(kafkaProducerTimerTask, 0, 1000);

        kafkaConsumer.subscribe(Collections.singleton(topic));

        final Gauge gauge = new Gauge.Builder()
                .name("kafka_synthetic_test_rtt")
                .help("Kafka synthetic test RTT")
                .labelNames("partition")
                .register();

        httpServer = new HTTPServer.Builder()
                .withDaemonThreads(true)
                .withHostname("0.0.0.0")
                .withPort(9191)
                .build();

        while (true) {
            try {
                ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(Duration.ofSeconds(10));

                long now = System.currentTimeMillis();

                for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                    long produceTime = Long.valueOf(consumerRecord.value());
                    System.out.println(String.format("partition [%d] rtt [%d] ms", consumerRecord.partition(), now - produceTime));
                    gauge.labels(String.valueOf(consumerRecord.partition())).set(now - produceTime);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public static class KafkaProducerTimerTask extends TimerTask {

        private String topic;
        private KafkaProducer<String, String> kafkaProducer;
        private Callback callback;

        public KafkaProducerTimerTask(String topic, KafkaProducer<String, String> kafkaProducer) {
            this.topic = topic;
            this.kafkaProducer = kafkaProducer;
            this.callback = new ProducerCallback();
        }

        public void run() {
            try {
                List<PartitionInfo> partitionInfoList = kafkaProducer.partitionsFor(topic);
                for (PartitionInfo partitionInfo : partitionInfoList) {
                    ProducerRecord<String, String> producerRecord =
                            new ProducerRecord<>(
                                    topic,
                                    partitionInfo.partition(),
                                    null,
                                    String.valueOf(System.currentTimeMillis()));

                    kafkaProducer.send(producerRecord, callback);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        private static class ProducerCallback implements Callback {
            @Override
            public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                if (e != null) {
                    e.printStackTrace();
                }
            }
        }
    }
}
