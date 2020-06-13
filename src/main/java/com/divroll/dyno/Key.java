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
 * Boxed {@linkplain String} key
 * associated with a {@linkplain Dyno} instance
 *
 * @author  Kerby Martino
 * @version 0-SNAPSHOT
 * @since   2020-06-15
 */
public class Key {
    private Key() {}

    private KeyBuilder builder;
    private String key;

    public Key(KeyBuilder builder, String key) {
        if(builder == null || key == null) {
            throw new IllegalArgumentException();
        }
        this.builder = builder;
        this.key = key;
    }

    /**
     * Retrieve the String key
     *
     * @return key
     */
    public String stringKey() {
        return this.key;
    }

    @Override
    public String toString() {
        return this.key;
    }

    /**
     * Checks if the key exists
     *
     * @return true if key exists, false if key does not exists
     */
    public boolean isExist() {
        return builder.dyno().isExists(this.key);
    }

    /**
     * Delete a key
     *
     * @return true if key was deleted, false if operation failed
     */
    public boolean delete() {
        return builder.dyno().delete(this.key);
    }

    /**
     * Get Entity
     *
     * @param valueType the type of this Entity value
     * @param <T> class type
     * @return null or the Entity
     */
    public <T> Entity getEntity(Class<T> valueType) {
        T value = builder.dyno().get(key, valueType);
        return EntityBuilder.create(builder.dyno())
                .build(key, value, valueType);
    }

    /**
     * Get Entity value
     *
     * @param clazz the class type to cast the value into
     * @param <T> class type
     * @return the value of the {@linkplain Entity}
     */
    public <T> T get(Class<T> clazz) {
        return (T) getEntity(clazz).getValue();
    }

}
