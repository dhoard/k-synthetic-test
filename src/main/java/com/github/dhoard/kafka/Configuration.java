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

import java.util.Properties;

public class Configuration {

    private Properties properties;

    public Configuration(Properties properties) {
        this.properties = properties;
    }

    public int asInt(String key) {
        String value = properties.getProperty(key);

        if ((value == null) || value.isBlank()) {
            throw new ConfigurationException("property \"" + key + "\" is required");
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ConfigurationException("property \"" + key + "\" must be an integer");
        }
    }

    public int asInt(String key, int defaultValue) {
        String value = properties.getProperty(key);

        if (value == null) {
            return defaultValue;
        }

        if (value.isBlank()) {
            throw new ConfigurationException("property \"" + key + "\" is required");
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ConfigurationException("property \"" + key + "\" must be an integer");
        }
    }

    public long asLong(String key) {
        String value = properties.getProperty(key);

        if ((value == null) || value.isBlank()) {
            throw new ConfigurationException("property \"" + key + "\" is required");
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new ConfigurationException("property \"" + key + "\" must be a long");
        }
    }

    public long asLong(String key, long defaultValue) {
        String value = properties.getProperty(key);

        if (value == null) {
            return defaultValue;
        }

        if (value.isBlank()) {
            throw new ConfigurationException("property \"" + key + "\" is required");
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new ConfigurationException("property \"" + key + "\" must be a long");
        }
    }

    public String asString(String key) {
        String value = properties.getProperty(key);

        if ((value == null) || value.isBlank()) {
            throw new ConfigurationException("property \"" + key + "\" is required");
        }

        return value.trim();
    }

    public String asString(String key, String defaultValue) {
        String value = properties.getProperty(key);

        if (value == null) {
            return defaultValue;
        }

        if (value.isBlank()) {
            throw new ConfigurationException("property \"" + key + "\" is required");
        }

        return value.trim();
    }
}
