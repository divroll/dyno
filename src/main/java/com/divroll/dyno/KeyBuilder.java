/*
 * Divroll, Platform for Hosting Static Sites
 * Copyright 2020, Divroll, and individual contributors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.divroll.dyno;

/**
 * Builds a {@linkplain Key} with parameters
 * and associated to a {@linkplain Dyno} instance.
 *
 * @author  Kerby Martino
 * @version 0-SNAPSHOT
 * @since   2020-06-15
 */
public final class KeyBuilder extends BuilderBase {

    private KeyBuilder() {}

    private String key = null;
    private Object value = null;
    private StringBuilder stringBuilder = null;

    public KeyBuilder(Dyno dyno) {
        if(dyno == null) {
            throw new IllegalArgumentException("Dyno cannot be null");
        }
        stringBuilder = new StringBuilder();
        this.dyno = dyno;
    }

    public static KeyBuilder create(Dyno dyno) {
        return new KeyBuilder(dyno);
    }

    /**
     * Append key
     *
     * @param keyName the name of the key
     * @param keyValue the key
     * @return the {@linkplain KeyBuilder}
     */
    public final KeyBuilder with(String keyName, String keyValue) {
        if(keyName == null || keyName.isEmpty()) {
            throw new IllegalArgumentException("Invalid key name");
        }
        if(keyValue == null || keyValue.isEmpty()) {
            throw new IllegalArgumentException("Invalid key value");
        }
        if(keyName.contains(dyno.getKeySpace())) {
            throw new IllegalArgumentException("Key name cannot contain key spacing character");
        }
        if(keyValue.contains(dyno.getKeySpace())) {
            throw new IllegalArgumentException("Key name cannot contain key spacing character");
        }
        if(stringBuilder.length() != 0) {
            stringBuilder.append(dyno.getKeySpace() + keyName + dyno.getKeySpace() + keyValue);
        } else {
            stringBuilder.append(keyName + dyno.getKeySpace() + keyValue);
        }
        return this;
    }

    /**
     * Append key name without key
     *
     * @param keyName the name of the key
     * @return the {@linkplain KeyBuilder}
     */
    public final KeyBuilder with(String keyName) {
        if(keyName == null || keyName.isEmpty()) {
            throw new IllegalArgumentException("Invalid key name");
        }
        if(stringBuilder.length() != 0) {
            stringBuilder.append(dyno.getKeySpace() + keyName);
        } else {
            stringBuilder.append(keyName);
        }
        return this;
    }

    /**
     * Build Key from methods
     *
     * @return the Key object
     */
    public Key build() {
        key = stringBuilder.toString();
        stringBuilder.setLength(0);
        return new Key(this, key);
    }

    /**
     * Build {@linkplain Key} with given key string parameter
     *
     * @param key string key to build
     * @return the Key object
     */
    public Key build(String key) {
        stringBuilder.setLength(0);
        this.key = key;
        return new Key(this, key);
    }

}
