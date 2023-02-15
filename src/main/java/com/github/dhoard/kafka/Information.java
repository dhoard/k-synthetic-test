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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Class to get application information
 */
public final class Information {

    private static final String RESOURCE_PATH = "/kafka-synthetic-test.properties";

    /**
     * Constructor
     */
    private Information() {
        // DO NOTHING
    }

    /**
     * Method to get a specific property from the application properties
     *
     * @param key
     * @param defaultValue
     * @return
     */
    private static String getProperty(String key, String defaultValue) {
        String value = defaultValue;

        try (InputStream inputStream = Information.class.getResourceAsStream(RESOURCE_PATH)) {
            if (inputStream != null) {
                Properties properties = new Properties();
                properties.load(inputStream);
                value = properties.getProperty(key, defaultValue).trim();
            }
        } catch (IOException e) {
            // DO NOTHING
        }

        return value;
    }

    /**
     * Method to get the application version
     *
     * @return
     */
    public static String getVersion() {
        return getProperty("version", "Unknown");
    }
}