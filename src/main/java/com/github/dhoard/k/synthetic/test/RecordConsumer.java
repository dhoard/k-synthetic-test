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

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * Class to consume records
 */
public class RecordConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordConsumer.class);

    private final Properties properties;
    private final String topic;
    private final Consumer<ConsumerRecord<String, String>> consumer;

    private Thread thread;
    private CountDownLatch countDownLatch;
    private KafkaConsumer<String, String> kafkaConsumer;

    private Timer assignPartitionsTimer;

    /**
     * Constructor
     *
     * @param configuration
     * @param consumer
     */
    public RecordConsumer(Configuration configuration, Consumer<ConsumerRecord<String, String>> consumer) {
        this.properties = configuration.toProperties();
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

                assignPartitions();

                countDownLatch = new CountDownLatch(2);

                thread = new Thread(this::poll);
                thread.start();

                assignPartitionsTimer = new Timer("assignment", true);
                assignPartitionsTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        assignPartitions();
                    }
                }, 0, 10000);

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

                assignPartitionsTimer.cancel();
                assignPartitionsTimer = null;

                kafkaConsumer.close();
                kafkaConsumer = null;

                thread = null;
                countDownLatch = null;
            }
        }
    }
    private void assignPartitions() {
        LOGGER.debug("assignPartitions()");

        synchronized (kafkaConsumer) {
            Set<TopicPartition> topicPartitionSet = new TreeSet<>(Comparator.comparingInt(TopicPartition::partition));

            List<PartitionInfo> partitionInfoList = kafkaConsumer.partitionsFor(topic);
            for (PartitionInfo partitionInfo : partitionInfoList) {
                topicPartitionSet.add(new TopicPartition(topic, partitionInfo.partition()));
            }

            Set<TopicPartition> existingTopicPartitionSet = kafkaConsumer.assignment();

            if (!Objects.equals(topicPartitionSet, existingTopicPartitionSet)) {
                LOGGER.debug("reassigning consumer partitions");
                kafkaConsumer.assign(topicPartitionSet);
                kafkaConsumer.seekToEnd(kafkaConsumer.assignment());
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
                    kafkaConsumer.poll(Duration.ofMillis(10000)).forEach(consumer);
                }
            } catch (Throwable t) {
                LOGGER.error("Exception consuming message", t);
            }
        }

        countDownLatch.countDown();
    }
}
