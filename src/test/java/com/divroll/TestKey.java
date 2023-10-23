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
package com.divroll;

import com.divroll.dyno.*;
import junit.framework.TestCase;
import org.fluttercode.datafactory.impl.DataFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestKey extends TestCase {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    Dyno dyno;

    @Before
    public void setup() {
        dyno = DynoClientBuilder
                .simple()
                .withEndpointConfig("s3.ap-southeast-1.wasabisys.com", "ap-southeast-1")
                .withCredentials(Credentials.getAccessKey(), Credentials.getSecretKey())
                .withBucket("test-database")
                .withKeySpace(DynoClientBuilder.DEFAULT_KEY_SPACE)
                .withBufferSize(1024)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullBuilder() {
        KeyBuilder.create(null)
                .build("testkey");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullKey() {
        KeyBuilder.create(dyno)
                .build(null);
    }

    @Test
    public void testDeleteEntity() {
        DataFactory df = new DataFactory();
        String stringKey = df.getRandomText(20);
        String stringValue = df.getRandomText(2048);
        Key key = EntityBuilder.create(dyno)
                .with("testkey", stringKey)
                .build(stringValue, String.class)
                .put();
        assertNotNull(key);
        assertTrue(key.delete());
    }

    @Test
    public void testIsExistsEntity() {
        DataFactory df = new DataFactory();
        String stringKey = df.getRandomText(20);
        String stringValue = df.getRandomText(2048);
        Key key = EntityBuilder.create(dyno)
                .with("testkey", stringKey)
                .build(stringValue, String.class)
                .put();
        assertNotNull(key);
        assertTrue(key.isExist());
        assertTrue(key.delete());
        assertFalse(key.isExist());
    }

    @Test
    public void testGetEntityValue() {
        // TODO
    }

    @Test
    public void testGetEntity() {
        // Put entity
        DataFactory df = new DataFactory();
        String stringKeyValue = df.getRandomText(20);
        String stringValue = df.getRandomText(2048);
        Key key = EntityBuilder.create(dyno)
                .with("testkey", stringKeyValue)
                .build(stringValue, String.class)
                .put();
        // Get entity with key
        Entity entity
                = key.getEntity(String.class);
        assertNotNull(entity);
        assertEquals("testkey" + DynoClientBuilder.DEFAULT_KEY_SPACE + stringKeyValue, entity.getKeyString());
        assertEquals(stringValue, entity.getValue(String.class));
    }

}
