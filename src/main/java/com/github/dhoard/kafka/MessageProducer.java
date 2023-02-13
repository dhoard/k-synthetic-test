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

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class MessageProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaSyntheticTest.class);

    private final String topic;
    private KafkaProducer<String, String> kafkaProducer;
    private final Timer timer;
    private boolean closed;

    public MessageProducer(Properties properties, String topic, long delay, long period) {
        this.closed = false;
        this.topic = topic;
        this.kafkaProducer = new KafkaProducer<>(properties);
        this.timer = new Timer();
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                produce();
            }
        }, delay, period);
    }

    private synchronized void produce() {
        LOGGER.debug("produce()");

        if (closed) {
            return;
        }

        List<PartitionInfo> partitionInfoList = kafkaProducer.partitionsFor(topic);
        for (PartitionInfo partitionInfo : partitionInfoList) {
            long now = System.currentTimeMillis();

            ProducerRecord<String, String> producerRecord =
                    new ProducerRecord<>(
                            topic,
                            partitionInfo.partition(),
                            null,
                            String.valueOf(now));

            kafkaProducer.send(producerRecord, (recordMetadata, e) -> {
                if (e != null) {
                    e.printStackTrace();
                }
            });
        }
    }

    public synchronized void close() {
        closed = true;

        kafkaProducer.close();
        kafkaProducer = null;
    }
}
