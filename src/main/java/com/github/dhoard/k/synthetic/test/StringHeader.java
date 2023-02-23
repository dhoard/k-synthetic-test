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

import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Class to implement a String based Kafka Header
 */
public final class StringHeader implements Header {

    private final String key;
    private final byte[] value;

    private StringHeader(String key, String value) {
        this.key = key;
        this.value = value.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public byte[] value() {
        return value;
    }

    public static StringHeader of(String key, String value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        return new StringHeader(key, value);
    }
}
