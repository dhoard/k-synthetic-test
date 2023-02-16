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

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

/**
 * Class to produce records
 */
public class RecordProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordProducer.class);

    private final Properties properties;
    private final long delayMs;
    private final long periodMs;
    private final String topic;
    private KafkaProducer<String, String> kafkaProducer;
    private Set<TopicPartition> topicPartitionSet;
    private Timer produceTimer;
    private Timer assignPartitionsTimer;


    /**
     * Constructor
     *
     * @param properties
     * @param delayMs
     * @param periodMs
     */
    public RecordProducer(Properties properties, long delayMs, long periodMs) {
        this.properties = properties;
        this.delayMs = delayMs;
        this.periodMs = periodMs;
        this.topic = (String) properties.remove("topic");
    }

    /**
     * Method to start the producer
     */
    public void start() {
        synchronized (this) {
            if (produceTimer == null) {
                LOGGER.info("starting producer");

                topicPartitionSet = new TreeSet<>(Comparator.comparingInt(TopicPartition::partition));

                kafkaProducer = new KafkaProducer<>(properties);

                produceTimer = new Timer("producer", true);
                produceTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        produce();
                    }
                }, delayMs, periodMs);

                assignPartitionsTimer = new Timer("assignment", true);
                assignPartitionsTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        assignPartitions();
                    }
                }, 0, 10000);

                LOGGER.info("producer started");
            }
        }
    }

    /**
     * Method to close the producer
     */
    public void close() {
        synchronized (this) {
            if (produceTimer != null) {
                produceTimer.cancel();

                kafkaProducer.close();
                kafkaProducer = null;

                produceTimer = null;
            }
        }
    }

    private void assignPartitions() {
        LOGGER.debug("assignPartitions()");

        synchronized (kafkaProducer) {
            Set<TopicPartition> newTopicPartitionSet = new TreeSet<>(Comparator.comparingInt(TopicPartition::partition));

            List<PartitionInfo> partitionInfoList = kafkaProducer.partitionsFor(topic);
            for (PartitionInfo partitionInfo : partitionInfoList) {
                newTopicPartitionSet.add(new TopicPartition(topic, partitionInfo.partition()));
            }

            if (!Objects.equals(topicPartitionSet, newTopicPartitionSet)) {
                LOGGER.debug("reassigning producer partitions");
                topicPartitionSet = newTopicPartitionSet;
            }
        }
    }

    /**
     * Method to produce records
     */
    private void produce() {
        LOGGER.debug("produce()");

        try {
            synchronized (this) {
                for (TopicPartition topicPartition : topicPartitionSet) {
                    long nowMs = System.currentTimeMillis();

                    ProducerRecord<String, String> producerRecord =
                            new ProducerRecord<>(
                                    topic,
                                    topicPartition.partition(),
                                    null,
                                    String.valueOf(nowMs));

                    kafkaProducer.send(producerRecord, (recordMetadata, e) -> {
                        if (e != null) {
                            LOGGER.error("Exception producing record", e);
                        }
                    });
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Exception producing record", t);
        }
    }
}
