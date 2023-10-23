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

import com.divroll.dyno.Dyno;
import com.divroll.dyno.DynoClientBuilder;
import com.divroll.dyno.EntityBuilder;
import com.divroll.dyno.Key;
import junit.framework.TestCase;
import org.fluttercode.datafactory.impl.DataFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestEntity extends TestCase {

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
    public void testPutEntity() {
        DataFactory df = new DataFactory();
        String stringKey = df.getRandomText(20);
        String stringValue = df.getRandomText(2048);
        Key key = EntityBuilder.create(dyno)
                .with("testkey", stringKey)
                .build(stringValue, String.class)
                .put();
        assertNotNull(key);
    }

    @Test
    public void testPutEntityIfAbsent() {
        DataFactory df = new DataFactory();
        String stringKey = df.getRandomText(20);
        String stringValue = df.getRandomText(2048);
        Key key = EntityBuilder.create(dyno)
                .with("testkey", stringKey)
                .build(stringValue, String.class)
                .putIfAbsent();
        assertNotNull(key);
    }

    @Test
    public void testPutEntityThenModify() {
        DataFactory df = new DataFactory();
        String stringKey = df.getRandomText(20);
        String stringValue = df.getRandomText(2048);
        Key key = EntityBuilder.create(dyno)
                .with("testkey", stringKey)
                .build(stringValue, String.class)
                .put();
        assertNotNull(key);
        String result = key.getEntity(String.class).getValueString();
        assertEquals(stringValue, result);
        //System.out.println(result);
        stringValue = "modified-" + stringValue;
        key = EntityBuilder.create(dyno)
                .with("testkey", stringKey)
                .build(stringValue, String.class)
                .put();
        result = key.getEntity(String.class).getValueString();
        assertEquals(stringValue, result);
        //System.out.println(result);
    }

}
