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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class MessageConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaSyntheticTest.class);

    private final AtomicBoolean closed;
    private KafkaConsumer<String, String> kafkaConsumer;
    private final Consumer<ConsumerRecords<String, String>> consumer;
    private final Thread thread;

    public MessageConsumer(Properties consumerProperties, String topic, Consumer<ConsumerRecords<String, String>> consumer) {
        this.closed = new AtomicBoolean();
        this.consumer = consumer;
        this.kafkaConsumer = new KafkaConsumer<>(consumerProperties);
        this.kafkaConsumer.subscribe(Collections.singleton(topic));
        this.thread = new Thread(this::consume);
        this.thread.start();
    }

    public void close() {
        closed.set(true);
    }

    private void consume() {
        while (!closed.get()) {
            LOGGER.debug("consume()");

            try {
                consumer.accept(kafkaConsumer.poll(Duration.ofSeconds(10)));
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
