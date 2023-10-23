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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.divroll.dyno.Dyno.uuid;

@RunWith(JUnit4.class)
public class TestKeyBuilder extends TestCase {

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
    public void testKeyBuilder() {
        String uuid = uuid();
        String testKey1 = KeyBuilder.create(dyno)
                .with("userid", uuid).build().stringKey();
        assertEquals("userid" + DynoClientBuilder.DEFAULT_KEY_SPACE + uuid, testKey1);
        String testKey2 = KeyBuilder.create(dyno)
                .with("userid", uuid)
                .with("username")
                .build().stringKey();
        assertEquals("userid" + DynoClientBuilder.DEFAULT_KEY_SPACE + uuid + DynoClientBuilder.DEFAULT_KEY_SPACE + "username", testKey2);
    }

}
