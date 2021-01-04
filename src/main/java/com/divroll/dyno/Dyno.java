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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import org.msgpack.MessagePack;

import java.io.*;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Dyno implements methods that allows easy key-value datastore
 * access to S3.
 *
 * @author  Kerby Martino
 * @version 0-SNAPSHOT
 * @since   2020-06-15
 */
public class Dyno {

    private AmazonS3 s3Client;
    private String bucketName;
    private String keySpace;
    private Integer bufferSize;

    public Dyno(String accessKey, String secretKey, String s3Endpoint, String region, String bucketName) {
        this(null, accessKey, secretKey, s3Endpoint, region, bucketName, null, null);
    }

    public Dyno(String accessKey, String secretKey, String s3Endpoint, String region, String bucketName, String keySpace) {
        this(null, accessKey, secretKey, s3Endpoint, region, bucketName, keySpace, null);
    }

    public Dyno(AmazonS3 s3Client, String accessKey, String secretKey, String s3Endpoint, String region, String bucketName, String keySpace, Integer bufferSize) {
        if(s3Client == null) {
            BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
            s3Client = AmazonS3ClientBuilder
                    .standard()
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s3Endpoint, region))
                    .build();
        }
        this.s3Client = s3Client;
        this.bucketName = bucketName == null ? DynoClientBuilder.DEFAULT_BUCKET_NAME : bucketName;
        this.keySpace = keySpace == null ? DynoClientBuilder.DEFAULT_KEY_SPACE : keySpace;
        this.bufferSize = bufferSize == null ? DynoClientBuilder.DEFAULT_BUFFER_SIZE : bufferSize;
    }

    /**
     * Puts an {@linkplain Entity} into the datastore
     *
     * @param entity the {@linkplain Entity} to put
     * @return true if operation was successful, false otherwise
     */
    public boolean put(Entity entity) {
        return put(entity.getKeyString(), entity.getValue(), entity.getValueType());
    }

    /**
     * Puts an {@linkplain Entity} into the datastore if it does not exists
     *
     * @param entity the {@linkplain Entity} to put
     * @return true if operation was successful, false otherwise
     */
    public boolean putIfAbsent(Entity entity) {
        return putIfAbsent(entity.getKeyString(), entity.getValue(), entity.getValueType());
    }

    /**
     * Get entity
     *
     * @param key the key of the value to get
     * @param valueType the type of value
     * @param <T> value class type
     * @return the {@linkplain Entity}
     */
    public <T> Entity getEntity(String key, Class<T> valueType) {
        Entity result = null;
        try {
            byte[] raw = getByte(key);
            MessagePack msgpack = new MessagePack();
            T value = msgpack.read(raw, valueType);
            result = EntityBuilder.create(this)
                    .build(key, value, valueType);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Get value by key
     *
     * @param key the string key of value to get
     * @param clazz class type of value
     * @param <T> type of value
     * @return the value
     */
    public <T> T get(String key, Class<T> clazz) {
        T result = null;
        try {
            byte[] raw = getByte(key);
            if(raw != null) {
                MessagePack msgpack = new MessagePack();
                result = msgpack.read(raw, clazz);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Get value as OutputStream by key
     *
     * @param key the string key of value to get
     * @param outputStream the stream output
     */
    public void get(String key, OutputStream outputStream) {
        getStream(key, outputStream);
    }

    /**
     * Put byte array value
     *
     * @param key the string key
     * @param value the byte array value to put
     * @return true if value was put, false if otherwise
     */
    public boolean put(String key, byte[] value) {
        try {
            return put(key, ByteSource.wrap(value).openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Put object by key.
     * This method only works with value of primitive types
     * and simple POJOs or non-nested POJO
     *
     * @param key string key of the value to put
     * @param value typed value to put
     * @param clazz type of value
     * @param <T> class type
     * @return true if value was put, false if otherwise
     */
    public <T> boolean put(String key, T value, Class<T> clazz) {
        boolean result = false;
        try {
            MessagePack msgpack = new MessagePack();
            if(!isPrimitive(clazz)) {
                msgpack.register(clazz);
            }
            byte[] raw = msgpack.write(value);
            result = put(key, raw);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    };

    /**
     * Put object by key if absent
     *
     * @param key string key of the value to put
     * @param value typed value to put
     * @param clazz type of value
     * @param <T> class type
     * @return true if value was put, false if otherwise
     */
    public <T> boolean putIfAbsent(String key, T value, Class<T> clazz) {
        if(isExists(key)) {
            return false;
        }
        return put(key, value, clazz);
    };

    /**
     * Put {@linkplain File} by key
     *
     * @param key string key of value to put
     * @param value {@linkplain File} value to put
     * @return true if value was put, false if otherwise
     */
    public boolean put(String key, File value) {
        try {
            return put(key, Files.asByteSource(value).openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Put {@linkplain File} by key if it does not exists
     *
     * @param key string key of value to put
     * @param value {@linkplain File} value to put
     * @return true if value was put, false if otherwise
     */
    public boolean putIfAbsent(String key, File value) {
        try {
            return putIfAbsent(key, Files.asByteSource(value).openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Put byte array value if key does not exists
     *
     * @param key the string key of the value to put
     * @param value the byte array value to put
     * @return true if value was put, false if otherwise
     */
    public boolean putIfAbsent(String key, byte[] value) {
        try {
            return putIfAbsent(key, ByteSource.wrap(value).openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Put {@linkplain Object} value if key does not exists
     * Objects must be annotated with {@linkplain org.msgpack.annotation.Message} annotation
     *
     * @param key the string key of the value to put
     * @param value the annotated {@linkplain Object} value to put
     * @return true if value was put, false if otherwise
     */
    public boolean putIfAbsent(String key, Object value) {
        boolean result = false;
        try {
            MessagePack msgpack = new MessagePack();
            byte[] raw = msgpack.write(value);
            result = putIfAbsent(key, raw);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Deletes a value by key
     *
     * @param key the key of the value to be deleted
     * @return true if key was deleted, false if otherwise
     */
    public boolean delete(String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(bucketName, key);
            s3Client.deleteObject(deleteObjectRequest);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Check if key exists
     *
     * @param key the key the check
     * @return true if key exists, false if otherwise
     */
    public boolean isExists(String key) {
        ListObjectsV2Result result = s3Client.listObjectsV2(bucketName, key);
        return result.getKeyCount() > 0;
    }

    /**
     * Get value as {@linkplain File}
     *
     * @param key the key of the value to get
     * @param file the {@linkplain File} to handle the value stream response
     */
    public void getFile(String key, File file) {
        try {
            OutputStream outputStream = new FileOutputStream(file);
            get(key, outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get value as {@linkplain String}
     *
     * @param key the key of the value to get
     * @return the {@linkplain String} value, null if key does not exists
     */
    public String getString(String key) {
        return get(key, String.class);
    }

    /**
     * Get value as {@linkplain Long}
     *
     * @param key the key of the value to get
     * @return the {@linkplain Long} value, null if key does not exists
     */
    public Long getLong(String key) {
        return get(key, Long.class);
    }

    /**
     * Get value as {@linkplain Integer}
     *
     * @param key the key of the value to get
     * @return the {@linkplain Integer} value, null if key does not exists
     */
    public Integer getInt(String key) {
        return get(key, Integer.class);
    }

    /**
     * Get value as {@linkplain Double}
     *
     * @param key the key of the value to get
     * @return the {@linkplain Double} value, null if key does not exists
     */
    public Double getDouble(String key) {
        return get(key, Double.class);
    }

    /**
     * Get value as {@linkplain Float}
     *
     * @param key the key of the value to get
     * @return the {@linkplain Float} value, null if key does not exists
     */
    public Float getFloat(String key) {
        return get(key, Float.class);
    }

    /**
     * Get value as {@linkplain Boolean}
     *
     * @param key the key of the value to get
     * @return the {@linkplain Boolean} value, null if key does not exists
     */
    public Boolean getBoolean(String key) {
        return get(key, Boolean.class);
    }

    /**
     * Put value as {@linkplain String}
     *
     * @param key the key of the value
     * @param value the {@linkplain String} value to put
     * @return true if value was put, false if otherwise
     */
    public boolean putString(String key, String value) {
        return put(key, value, String.class);
    }

    /**
     * Put value as {@linkplain Long}
     *
     * @param key the key of the value
     * @param value the {@linkplain Long} value to put
     * @return true if value was put, false if otherwise
     */
    public boolean putLong(String key, Long value) {
        return put(key, value, Long.class);
    }

    /**
     * Put value as {@linkplain Integer}
     *
     * @param key the key of the value
     * @param value the {@linkplain Integer} value to put
     * @return true if value was put, false if otherwise
     */
    public boolean putInt(String key, Integer value) {
        return put(key, value, Integer.class);
    }

    /**
     * Put value as {@linkplain Double}
     *
     * @param key the key of the value
     * @param value the {@linkplain Double} value to put
     * @return true if value was put, false if otherwise
     */
    public boolean putDouble(String key, Double value) {
        return put(key, value, Double.class);
    }

    /**
     * Put value as {@linkplain Float}
     *
     * @param key the key of the value
     * @param value the {@linkplain Float} value to put
     * @return true if value was put, false if otherwise
     */
    public boolean putFloat(String key, Float value) {
        return put(key, value, Float.class);
    }

    /**
     * Put value as {@linkplain Boolean}
     *
     * @param key the key of the value
     * @param value the {@linkplain Boolean} value to put
     * @return true if value was put, false if otherwise
     */
    public boolean putBoolean(String key, Boolean value) {
        return put(key, value, Boolean.class);
    }

    /**
     * Put value as {@linkplain long}
     *
     * @param key the key of the value
     * @param value the {@linkplain long} value to put
     * @return true if value was put, false if otherwise
     */
    public boolean putLong(String key, long value) {
        return put(key, value, Long.class);
    }

    /**
     * Put value as {@linkplain int}
     *
     * @param key the key of the value
     * @param value the {@linkplain int} value to put
     * @return true if value was put, false if otherwise
     */
    public boolean putInt(String key, int value) {
        return put(key, value, Integer.class);
    }

    /**
     * Put value as {@linkplain double}
     *
     * @param key the key of the value
     * @param value the {@linkplain double} value to put
     * @return true if value was put, false if otherwise
     */
    public boolean putDouble(String key, double value) {
        return put(key, value, Double.class);
    }

    /**
     * Put value as {@linkplain float}
     *
     * @param key the key of the value
     * @param value the {@linkplain float} value to put
     * @return true if value was put, false if otherwise
     */
    public boolean putFloat(String key, float value) {
        return put(key, value, Float.class);
    }

    /**
     * Put value as {@linkplain boolean}
     *
     * @param key the key of the value
     * @param value the {@linkplain boolean} value to put
     * @return true if value was put, false if otherwise
     */
    public boolean putBoolean(String key, boolean value) {
        return put(key, value, Boolean.class);
    }

    public List<Key> listKeys(int maxKeys) {
        List<Key> keys = new LinkedList<>();
        if(s3Client != null) {
            ListObjectsV2Request req  = new ListObjectsV2Request().withBucketName(bucketName).withMaxKeys(maxKeys);
            ListObjectsV2Result result;
            do {
                result = s3Client.listObjectsV2(req);
                for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                    keys.add(KeyBuilder.create(this).build(objectSummary.getKey()));
                }
            } while (result.isTruncated());
        }
        return keys;
    }

    public List<Key> listKeys(String prefix, int maxKeys) {
        List<Key> keys = new LinkedList<>();
        if(s3Client != null) {
            ListObjectsV2Request req  = new ListObjectsV2Request().withBucketName(bucketName)
                    .withPrefix(prefix)
                    .withMaxKeys(maxKeys);
            ListObjectsV2Result result;
            do {
                result = s3Client.listObjectsV2(req);
                for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                    keys.add(KeyBuilder.create(this).build(objectSummary.getKey()));
                }
            } while (result.isTruncated());
        }
        return keys;
    }

    /**
     * Get the associated {@linkplain KeyBuilder}
     *
     * @return the key builder
     */
    public KeyBuilder getKeyBuilder() {
        return new KeyBuilder(this);
    }

    /**
     * Get the associated {@linkplain EntityBuilder}
     *
     * @return the entity builder
     */
    public EntityBuilder getEntityBuilder() {
        return new EntityBuilder(this);
    }

    public static String uuid() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    public static String sha256(String string) {
        HashCode hashCode
                = Hashing.sha256().hashString(string, Charset.defaultCharset());
        return hashCode.toString();
    }

    /**
     * Get key spacing character
     *
     * @return key spacing character
     */
    public String getKeySpace() {
        return keySpace;
    }

    /**
     * Get size of buffer
     *
     * @return the configured buffer size
     */
    public Integer getBufferSize() {
        return bufferSize;
    }

    /**
     * Put {@linkplain InputStream} value if key does not exists
     *
     * @param key the string key to put
     * @param value the {@linkplain InputStream} value to put
     * @return true if value was put, false if otherwise
     */
    public boolean putIfAbsent(String key, InputStream value) {
        if(key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be empty or null");
        }
        if(isExists(key)) {
            return false;
        }
        return put(key, value);
    }

    /**
     * Put {@linkplain InputStream} value
     *
     * @param key the string key to put
     * @param value the {@linkplain InputStream} value to put
     * @return true if value was put, false if otherwise
     */
    public boolean put(String key, InputStream value) {
        if(value == null || key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key and/or value cannot be empty or null");
        }
        try {
            if(s3Client != null) {
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType("application/octet-stream");
                // TODO: WARNING: No content length specified for stream data.
                // TODO: Stream contents will be buffered in memory and could result in out of memory errors.
                // TODO: add `metadata.setContentLength(file.length());`
                PutObjectRequest request = new PutObjectRequest(bucketName, key, value, metadata);
                request.setMetadata(metadata);
                PutObjectResult result = s3Client.putObject(request);
                return result != null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get value as byte array by key
     *
     * @param key the key string to get
     * @return value as byte array
     */
    public byte[] getByte(String key) {
        byte[] result = null;
        try {
            if(s3Client != null) {
                GetObjectRequest request = new GetObjectRequest(bucketName, key);
                S3Object s3Object = s3Client.getObject(request);
                if(s3Object != null) {
                    InputStream inputStream = s3Object.getObjectContent();
                    result = ByteStreams.toByteArray(inputStream);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Get value as {@linkplain OutputStream} by key
     *
     * @param key string key to get
     * @param outputStream stream to handle response from S3 service
     */
    private void getStream(String key, OutputStream outputStream) {
        try {
            if(s3Client != null) {
                GetObjectRequest request = new GetObjectRequest(bucketName, key);
                S3Object s3Object = s3Client.getObject(request);
                InputStream inputStream = s3Object.getObjectContent();
                byte[] buf = new byte[bufferSize != null ? bufferSize : DynoClientBuilder.DEFAULT_BUFFER_SIZE];
                int numRead;
                while ( (numRead = inputStream.read(buf) ) >= 0) {
                    outputStream.write(buf, 0, numRead);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if class is a Java primitive type
     *
     * @param clazz the class to check
     * @return true if class is primitive, false if class is not primitive
     */
    private boolean isPrimitive(Class clazz) {
        if(clazz.equals(String.class)
                || clazz.equals(Number.class)
                || clazz.equals(Short.class)
                || clazz.equals(Long.class)
                || clazz.equals(Double.class)
                || clazz.equals(Float.class)
                || clazz.equals(Integer.class)
                || clazz.equals(Boolean.class)
        ) {
           return true;
        }
        return false;
    }

}
