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
import com.divroll.model.User;
import com.divroll.model.UserProfile;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import junit.framework.TestCase;
import org.fluttercode.datafactory.impl.DataFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.*;
import java.nio.charset.Charset;

import static com.divroll.dyno.Dyno.sha256;
import static com.divroll.dyno.Dyno.uuid;

@RunWith(JUnit4.class)
public class TestDyno extends TestCase {

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
    public void testPutStream() throws IOException {
        String message = "Hello, world.";
        boolean result
                = dyno.put("hello_world", ByteSource.wrap(message.getBytes(Charset.defaultCharset())).openStream());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        dyno.get("hello_world", outputStream);
        byte[] messageBytes = outputStream.toByteArray();
        assertTrue(result);
        assertEquals(message, new String(messageBytes, Charset.defaultCharset()));
    }

    @Test
    public void testPutInputstreamIfAbsent() throws IOException {
        String message = "Hello, world.";
        String key = "hello_world";
        boolean result
                = dyno.putIfAbsent(key, ByteSource.wrap(message.getBytes(Charset.defaultCharset())).openStream());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        dyno.get(key, outputStream);
        byte[] messageBytes = outputStream.toByteArray();
        assertFalse(result);
        dyno.delete(key);
        result = dyno.putIfAbsent(key, ByteSource.wrap(message.getBytes(Charset.defaultCharset())).openStream());
        assertTrue(result);
        assertEquals(message, new String(messageBytes, Charset.defaultCharset()));
    }

    @Test
    public void testPutEntity() {
        String username = "dino";
        String passwordHash = sha256("hardtoguess");
        // Create user with password
        String userId = uuid();
        EntityBuilder builder = EntityBuilder.create(dyno);
        assertNotNull(builder.with("username", username)
                .with("user_id")
                .build(userId, String.class)
                .putIfAbsent());
    }

    @Test
    public void testPutEntityIfAbsent() {
        DataFactory df = new DataFactory();
        String username = df.getRandomWord(20);
        String passwordHash = sha256(df.getRandomText(20));
        // Create user with password
        String userId = uuid();
        EntityBuilder builder = EntityBuilder.create(dyno);
        assertNotNull(builder.with("username", username)
                .with("user_id")
                .build(userId, String.class)
                .putIfAbsent());
        assertNotNull(builder.with("user_id", userId)
                .with("username", username)
                .with("password")
                .build(passwordHash, String.class)
                .putIfAbsent());
        assertNull(builder.with("username", username)
                .with("user_id")
                .build(userId, String.class)
                .putIfAbsent());
    }

    @Test
    public void testGetEntity() {
        String username = "dino";
        String userId = KeyBuilder.create(dyno)
                .with("username", username)
                .with("user_id")
                .build()
                .get(String.class);
        assertNotNull(userId);
        Entity entity = KeyBuilder.create(dyno)
                .with("username", username)
                .with("user_id")
                .build()
                .getEntity(String.class);
        assertEquals(userId, entity.getValue());
        KeyBuilder.create(dyno)
                .with("username", username)
                .with("user_id")
                .build()
                .delete();
    }

    @Test
    public void testGetAsType() {
        DataFactory df = new DataFactory();
        String key = df.getRandomChars(20);
        String value = df.getRandomWord(100);
        assertTrue(dyno.putIfAbsent(key, value));
        String result = dyno.get(key, String.class);
        assertEquals(value, result);
    }

    @Test
    public void testGetStream() throws IOException {
        DataFactory df = new DataFactory();
        String key = df.getRandomChars(20);
        String message = df.getRandomWord(100);
        boolean result
                = dyno.put(key, ByteSource.wrap(message.getBytes(Charset.defaultCharset())).openStream());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        dyno.get(key, outputStream);
        byte[] messageBytes = outputStream.toByteArray();
        assertTrue(result);
        assertEquals(message, new String(messageBytes, Charset.defaultCharset()));
    }

    @Test
    public void testPutObject() {
        DataFactory df = new DataFactory();
        User user = new User();
        user.setUsername(df.getRandomText(20));
        user.setPassword(sha256(df.getRandomChars(20)));
        UserProfile userProfile = new UserProfile();
        userProfile.setFirstName(df.getFirstName());
        userProfile.setLastName(df.getLastName());
        user.setUserProfile(userProfile);
        // Create user with password
        String userId = uuid();
        EntityBuilder builder = EntityBuilder.create(dyno);
        if(builder.with("user_id", userId)
                .with("user")
                .build(user, User.class)
                .put() != null) {
            Entity entity = KeyBuilder.create(dyno)
                    .with("user_id", userId)
                    .with("user")
                    .build()
                    .getEntity(User.class);
            assertNotNull(entity);
            User result = (User) entity.getValue(User.class);
            UserProfile resultProfile = result.getUserProfile();
            assertNotNull(result);
            assertNotNull(resultProfile);
            assertEquals(user.getUsername(), result.getUsername());
            assertEquals(user.getPassword(), result.getPassword());
            assertEquals(userProfile.getFirstName(), resultProfile.getFirstName());
            assertEquals(userProfile.getLastName(), resultProfile.getLastName());
        }
    }

    @Test
    public void testPutObjectIfAbsent() {
        DataFactory df = new DataFactory();
        User user = new User();
        user.setUsername(df.getRandomText(20));
        user.setPassword(sha256(df.getRandomChars(20)));
        UserProfile userProfile = new UserProfile();
        userProfile.setFirstName(df.getFirstName());
        userProfile.setLastName(df.getLastName());
        user.setUserProfile(userProfile);
        // Create user with password
        String userId = uuid();
        EntityBuilder builder = EntityBuilder.create(dyno);
        if(builder.with("user_id", userId)
                .with("user")
                .build(user, User.class)
                .putIfAbsent() != null) {
            Entity entity = KeyBuilder.create(dyno)
                    .with("user_id", userId)
                    .with("user")
                    .build()
                    .getEntity(User.class);
            assertNotNull(entity);
            User result = (User) entity.getValue(User.class);
            UserProfile resultProfile = result.getUserProfile();
            assertNotNull(result);
            assertNotNull(resultProfile);
            assertEquals(user.getUsername(), result.getUsername());
            assertEquals(user.getPassword(), result.getPassword());
            assertEquals(userProfile.getFirstName(), resultProfile.getFirstName());
            assertEquals(userProfile.getLastName(), resultProfile.getLastName());
        }
        assertNull(builder.with("user_id", userId)
                .with("user")
                .build(user, User.class)
                .putIfAbsent());
    }

    @Test
    public void testPutAsType() {
        DataFactory df = new DataFactory();
        String key = df.getRandomText(20);
        String value = df.getRandomText(100);
        assertTrue(dyno.put(key, value, String.class));
        assertEquals(value, dyno.get(key, String.class));
    }

    @Test
    public void testPutAsTypeIfAbsent() {
        DataFactory df = new DataFactory();
        String key = df.getRandomText(20);
        String value = df.getRandomText(100);
        assertTrue(dyno.put(key, value, String.class));
        assertEquals(value, dyno.get(key, String.class));
        assertFalse(dyno.putIfAbsent(key, value, String.class));
        dyno.delete(key);
        assertTrue(dyno.putIfAbsent(key, value, String.class));
    }

    @Test
    public void testPutFile() throws IOException {
        DataFactory df = new DataFactory();
        String key = df.getRandomText(30);
        String fileContent = df.getRandomWord(1024);
        File file = File.createTempFile(df.getRandomWord(20), ".txt");
        BufferedWriter out = new BufferedWriter(new FileWriter(file));
        out.write(fileContent);
        out.close();
        System.out.println(file.getPath());
        assertTrue(dyno.put(key, file));
        File resultFile = File.createTempFile(df.getRandomWord(20), ".txt");
        dyno.getFile(key, resultFile);
        ByteSource source = Files.asByteSource(file);
        byte[] result = source.read();
        assertEquals(fileContent, new String(result));
    }

    @Test
    public void testPutFileIfAbsent() throws IOException {
        DataFactory df = new DataFactory();
        String key = df.getRandomText(20);
        String fileContent = df.getRandomWord(10240);
        File file = File.createTempFile(df.getRandomWord(20), ".txt");
        BufferedWriter out = new BufferedWriter(new FileWriter(file));
        out.write(fileContent);
        out.close();
        System.out.println(file.getPath());
        assertTrue(dyno.putIfAbsent(key, file));

        File resultFile = File.createTempFile(df.getRandomWord(20), ".txt");
        dyno.getFile(key, resultFile);
        ByteSource source = Files.asByteSource(file);
        byte[] result = source.read();
        assertEquals(fileContent, new String(result));
    }

    @Test
    public void testDelete() {
        DataFactory df = new DataFactory();
        String key = df.getRandomText(20);
        String value = df.getRandomText(1024);
        assertTrue(dyno.put(key, value, String.class));
        assertTrue(dyno.delete(key));
    }

    @Test
    public void testIsExists() {
        DataFactory df = new DataFactory();
        String key = df.getRandomText(20);
        String value = df.getRandomText(1024);
        assertFalse(dyno.isExists(key));
        assertTrue(dyno.put(key, value, String.class));
        assertTrue(dyno.isExists(key));
    }

    @Test
    public void testPutGetString() {
        DataFactory df = new DataFactory();
        String key = df.getRandomText(20);
        String value = df.getRandomText(1024);
        assertTrue(dyno.putString(key, value));
        assertEquals(value, dyno.getString(key));
    }

    @Test
    public void testPutGetLong() {
        DataFactory df = new DataFactory();
        String key = df.getRandomText(20);
        Long value = 1234567890L;
        assertTrue(dyno.putLong(key, value));
        assertEquals(value, dyno.getLong(key));
    }

    @Test
    public void testPutGetInt() {
        DataFactory df = new DataFactory();
        String key = df.getRandomText(20);
        Integer value = 1234567890;
        assertTrue(dyno.putInt(key, value));
        assertEquals(value, dyno.getInt(key));
    }

    @Test
    public void testPutGetDouble() {
        DataFactory df = new DataFactory();
        String key = df.getRandomText(20);
        Double value = 123.456;
        assertTrue(dyno.putDouble(key, value));
        assertEquals(value, dyno.getDouble(key));
    }

    @Test
    public void testPutGetFloat() {
        DataFactory df = new DataFactory();
        String key = df.getRandomText(20);
        Float value = 123.456F;
        assertTrue(dyno.putFloat(key, value));
        assertEquals(value, dyno.getFloat(key));
    }

    @Test
    public void testOPutGetBoolean() {
        DataFactory df = new DataFactory();
        String key = df.getRandomText(20);
        assertTrue(dyno.putBoolean(key, false));
        assertFalse(dyno.getBoolean(key));
    }

    @Test
    public void testGetByte() {
        DataFactory df = new DataFactory();
        String key = df.getRandomText(20);
        String value = df.getRandomText(1024);
        byte[] toPut = value.getBytes(Charset.defaultCharset());
        assertTrue(dyno.put(key, toPut));
        byte[] bytes = dyno.getByte(key);
        assertNotNull(bytes);
        assertEquals(toPut.length, bytes.length);
    }

}
