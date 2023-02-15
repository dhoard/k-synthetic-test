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

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * Class to consume records
 */
public class RecordConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordConsumer.class);

    private final Properties properties;
    private final String topic;
    private final Consumer<ConsumerRecords<String, String>> consumer;

    private Thread thread;
    private CountDownLatch countDownLatch;
    private KafkaConsumer<String, String> kafkaConsumer;
    private List<TopicPartition> topicPartitionList;

    /**
     * Constructor
     *
     * @param properties
     * @param consumer
     */
    public RecordConsumer(Properties properties, Consumer<ConsumerRecords<String, String>> consumer) {
        this.properties = properties;
        this.consumer = consumer;
        this.topic = (String) properties.remove("topic");
    }

    /**
     * Method to start the consumer
     */
    public void start() {
        synchronized (this) {
            if (thread == null) {
                LOGGER.info("starting consumer");

                kafkaConsumer = new KafkaConsumer<>(properties);

                // Manual partition assignment

                topicPartitionList = new ArrayList<>();

                List<PartitionInfo> partitionInfoList = kafkaConsumer.partitionsFor(topic);
                for (PartitionInfo partitionInfo : partitionInfoList) {
                    topicPartitionList.add(new TopicPartition(topic, partitionInfo.partition()));
                }

                kafkaConsumer.assign(topicPartitionList);
                kafkaConsumer.seekToEnd(kafkaConsumer.assignment());

                countDownLatch = new CountDownLatch(2);

                thread = new Thread(this::poll);
                thread.start();

                LOGGER.info("consumer started");
            }
        }
    }

    /**
     * Method to close the consumer
     */
    public void close() {
        synchronized (this) {
            if (thread != null) {
                countDownLatch.countDown();

                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    // DO NOTHING
                }

                kafkaConsumer.close();
                kafkaConsumer = null;

                thread = null;
                countDownLatch = null;
            }
        }
    }

    /**
     * Method to poll for records
     */
    private void poll() {
        LOGGER.debug("poll()");

        while (countDownLatch.getCount() == 2) {
            try {
                synchronized (kafkaConsumer) {
                    consumer.accept(kafkaConsumer.poll(10000));
                }
            } catch (Throwable t) {
                LOGGER.error("Exception consuming message", t);
            }
        }

        countDownLatch.countDown();
    }
}
