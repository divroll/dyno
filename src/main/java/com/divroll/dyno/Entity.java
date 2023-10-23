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
 * Boxed {@linkplain String} key and {@linkplain Object} value
 * associated with a {@linkplain Dyno} instance
 *
 * @author  Kerby Martino
 * @version 0-SNAPSHOT
 * @since   2020-06-15
 */
public class Entity<T> {

    private String key;
    private T value;
    private EntityBuilder builder;
    private Class<T> clazz;

    private Entity() {}

    public Entity(EntityBuilder builder, String key, T value, Class<T> clazz) {
        this.key = key;
        this.value = value;
        this.builder = builder;
        this.clazz = clazz;
    }

    /**
     * Get the key
     *
     * @return key as String
     */
    public String getKeyString() {
        return key;
    }

    /**
     * Get the value
     *
     * @return value as a String
     */
    public String getValueString() {
        return (String) value;
    }

    /**
     * Get value
     *
     * @return value as an Object
     */
    public T getValue() {
        return value;
    }

    /**
     * Get value type
     *
     * @return class type of value
     */
    public Class<T> getValueType() {
        return clazz;
    }

    /**
     * Get value
     *
     * @param clazz the class type to cast the value into
     * @param <T> class type
     * @return value casted as type
     */
    public <T> T getValue(Class<T> clazz) {
        return (T) value;
    }

    /**
     * Get key
     *
     * @return the Key of this Entity
     */
    public Key getKey() {
        return KeyBuilder.create(builder.dyno())
                .build(getKeyString());
    }

    /**
     * Put entity
     *
     * @return the Key
     */
    public Key put() {
        if(builder.dyno().put(this)) {
            return KeyBuilder
                    .create(builder.dyno())
                    .build(getKeyString());
        }
        return null;
    }

    /**
     * Put entity if not exists
     *
     * @return the Key
     */
    public Key putIfAbsent() {
        if(builder.dyno().putIfAbsent(this)) {
            return KeyBuilder
                    .create(builder.dyno())
                    .build(getKeyString());
        }
        return null;
    }

}
