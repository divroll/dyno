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
import com.divroll.dyno.Entity;
import com.divroll.dyno.EntityBuilder;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestEntityBuilder extends TestCase {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    Dyno dyno;

    @Before
    public void setup() {
        dyno = DynoClientBuilder
                .simple()
                .withEndpointConfig("s3.wasabisys.com", "us-east-1")
                .withCredentials(Credentials.getAccessKey(), Credentials.getSecretKey())
                .withBucket("s3dyno")
                .withKeySpace(DynoClientBuilder.DEFAULT_KEY_SPACE)
                .withBufferSize(1024)
                .build();
    }

    @Test
    public void testEntityBuilder() {
        Entity entity = EntityBuilder.create(dyno)
                .with("foo").build("bar", String.class);
        assertNotNull(entity);
        assertEquals("foo", entity.getKeyString());
        assertEquals("bar", entity.getValue());
        entity = EntityBuilder.create(dyno)
                .with("foo")
                .with("baz", "bam")
                .build("bar", String.class);
        assertNotNull(entity);
        assertEquals("foo" + DynoClientBuilder.DEFAULT_KEY_SPACE
                + "baz" + DynoClientBuilder.DEFAULT_KEY_SPACE  + "bam",  entity.getKeyString());
        assertEquals("bar", entity.getValue(String.class));
    }

}
