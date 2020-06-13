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
 * Builds an {@linkplain Entity} with parameters
 * and associated to a {@linkplain Dyno} instance.
 *
 * @author  Kerby Martino
 * @version 0-SNAPSHOT
 * @since   2020-06-15
 */
public final class EntityBuilder extends BuilderBase {

    private EntityBuilder() {}

    private String key = null;
    private Object value = null;
    private StringBuilder stringBuilder = null;

    public EntityBuilder(Dyno dyno) {
        stringBuilder = new StringBuilder();
        this.dyno = dyno;
    }

    public static EntityBuilder create(Dyno dyno) {
        return new EntityBuilder(dyno);
    }

    public final EntityBuilder with(String keyName, String keyValue) {
        if(keyName == null || keyName.isEmpty()) {
            throw new IllegalArgumentException("Invalid key name");
        }
        if(keyValue == null || keyValue.isEmpty()) {
            throw new IllegalArgumentException("Invalid key value");
        }
        if(stringBuilder == null) {
            throw new IllegalArgumentException("Key already finalized");
        }
        if(stringBuilder.length() != 0) {
            stringBuilder.append(dyno.getKeySpace() + keyName + dyno.getKeySpace() + keyValue);
        } else {
            stringBuilder.append(keyName + dyno.getKeySpace() + keyValue);
        }
        return this;
    }

    public final EntityBuilder with(String keyName) {
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

    public <T> Entity build(String key, Object value, Class<T> clazz) {
        stringBuilder.setLength(0);
        stringBuilder.append(key);
        this.key = stringBuilder.toString();
        this.value = value;
        return new Entity(this, this.key, this.value, clazz);
    }

    public <T> Entity build(Object value, Class<T> clazz) {
        this.value = value;
        this.key = stringBuilder.toString();
        stringBuilder.setLength(0);
        return new Entity(this, this.key, this.value, clazz);
    }

}
