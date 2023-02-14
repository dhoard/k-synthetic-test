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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class MessageConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaSyntheticTest.class);

    private final AtomicBoolean closed;
    private final long periodMs;
    private KafkaConsumer<String, String> kafkaConsumer;
    private final Consumer<ConsumerRecords<String, String>> consumer;
    private final Thread thread;

    public MessageConsumer(Properties consumerProperties, String topic, long periodMs, Consumer<ConsumerRecords<String, String>> consumer) {
        LOGGER.info("starting consumer");

        this.closed = new AtomicBoolean();
        this.periodMs = periodMs;
        this.consumer = consumer;
        this.kafkaConsumer = new KafkaConsumer<>(consumerProperties);

        // Manual partition assignment

        List<TopicPartition> topicPartitionList = new ArrayList<>();
        List<PartitionInfo> partitionInfoList = this.kafkaConsumer.partitionsFor(topic);
        for (PartitionInfo partitionInfo : partitionInfoList) {
            topicPartitionList.add(new TopicPartition(topic, partitionInfo.partition()));
        }

        this.kafkaConsumer.assign(topicPartitionList);
        this.kafkaConsumer.seekToEnd(this.kafkaConsumer.assignment());
        this.thread = new Thread(this::consume);
        this.thread.start();

        LOGGER.info("consumer started");
    }

    public void close() {
        closed.set(true);
    }

    private void consume() {
        while (!closed.get()) {
            try {
                consumer.accept(kafkaConsumer.poll(periodMs));
            } catch (Throwable t) {
                t.printStackTrace();

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    // DO NOTHING
                }
            }
        }

        kafkaConsumer.close();
        kafkaConsumer = null;
    }
}
