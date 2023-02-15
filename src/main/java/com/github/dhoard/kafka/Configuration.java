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

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

/**
 * Class to implement configuration
 */
public class Configuration {

    private Properties properties;

    /**
     * Constructor
     */
    public Configuration() {
        properties = new Properties();
    }

    /**
     * Method to load Properties from a file
     *
     * @param filename
     */
    public void load(String filename) throws IOException {
        Properties properties = new Properties();

        try (Reader reader = new FileReader(filename)) {
            properties.load(reader);
        }

        this.properties = properties;
    }

    /**
     * Method to determine if a key exists
     *
     * @param key
     * @return
     */
    public boolean containsKey(String key) {
        return properties.containsKey(key);
    }

    /**
     * Method to put a key / value
     *
     * @param key
     * @param value
     * @return
     */
    public String put(String key, String value) {
        properties.put(key, value);
        return value;
    }

    /**
     * Method to remove a key, returning the value
     *
     * @param key
     * @return
     */
    public String remove(String key) {
        return (String) properties.remove(key);
    }

    /**
     * Method to get a required value as an int
     *
     * @param key
     * @return
     */
    public int asInt(String key) {
        String value = properties.getProperty(key);

        if ((value == null) || value.isBlank()) {
            throw new ConfigurationException("property \"" + key + "\" is required");
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ConfigurationException("property \"" + key + "\" must be an integer", e);
        }
    }

    /**
     * Method to get an optional value as an int
     *
     * @param key
     * @param defaultValue
     * @return
     */
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
            throw new ConfigurationException("property \"" + key + "\" must be an integer", e);
        }
    }

    /**
     * Method to get a required value as a long
     *
     * @param key
     * @return
     */
    public long asLong(String key) {
        String value = properties.getProperty(key);

        if ((value == null) || value.isBlank()) {
            throw new ConfigurationException("property \"" + key + "\" is required");
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new ConfigurationException("property \"" + key + "\" must be a long", e);
        }
    }

    /**
     * Method to get an optional value as a long
     *
     * @param key
     * @param defaultValue
     * @return
     */
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
            throw new ConfigurationException("property \"" + key + "\" must be a long", e);
        }
    }

    /**
     * Method to get a required value as a String
     *
     * @param key
     * @return
     */
    public String asString(String key) {
        String value = properties.getProperty(key);

        if ((value == null) || value.isBlank()) {
            throw new ConfigurationException("property \"" + key + "\" is required");
        }

        return value.trim();
    }

    /**
     * Method to get an optional value as a String
     *
     * @param key
     * @param defaultValue
     * @return
     */
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

    /**
     * Method to return Properties (deep copy)
     *
     * @return
     */
    public Properties toProperties() {
        return copy().properties;
    }

    /**
     * Method to copy the Configuration object (deep copy)
     *
     * @return
     */
    public Configuration copy() {
        Configuration configuration = new Configuration();
        configuration.properties.putAll(this.properties);
        return configuration;
    }
}
