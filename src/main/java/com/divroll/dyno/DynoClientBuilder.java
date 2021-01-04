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

import com.amazonaws.services.s3.AmazonS3;

/**
 * Builds a {@linkplain Dyno} instance with configuration
 *
 * @author  Kerby Martino
 * @version 0-SNAPSHOT
 * @since   2020-06-15
 */
public final class DynoClientBuilder {

    public static final String DEFAULT_S3_ENDPOINT = "s3.wasabisys.com";
    public static final String DEFAULT_S3_REGION = "us-east-1";
    public static final String DEFAULT_BUCKET_NAME = "s3dyno";
    public static final String DEFAULT_KEY_SPACE = ":";
    public static final int DEFAULT_BUFFER_SIZE = 1024;

    private AmazonS3 s3client;
    private String accessKey;
    private String secretKey;
    private String s3Endpoint;
    private String region;
    private String bucketName;
    private String keySpace;
    private Integer bufferSize;
    private boolean hashKeys = false;
    private boolean encryptValues = false;

    private DynoClientBuilder() {}

    public DynoClientBuilder(String accessKey, String secretKey, String s3Endpoint, String region, String bucketName, String keySpace, Integer bufferSize) {}

    public static DynoClientBuilder simple() {
        return new DynoClientBuilder(null, null, null, null, null, null, null);
    }

    public final DynoClientBuilder withClient(AmazonS3 s3Client) {
        this.s3client = s3Client;
        return this;
    }

    /**
     * Set S3 credentials
     *
     * @param accessKey S3 access key
     * @param secretKey S3 secret key
     * @return the client builder
     */
    public final DynoClientBuilder withCredentials(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        return this;
    }

    /**
     * Set the endpoint URL and region
     *
     * @param s3Endpoint S3 service endpoint URL
     * @param region S3 service region
     * @return the client builder
     */
    public final DynoClientBuilder withEndpointConfig(String s3Endpoint, String region) {
        this.s3Endpoint = s3Endpoint;
        this.region = region;
        return this;
    }

    public final DynoClientBuilder withConfiguration(boolean hashKeys, boolean encryptValues) {
        // TODO
        this.hashKeys = hashKeys;
        this.encryptValues = encryptValues;
        return this;
    }

    /**
     * Set the key spacing
     *
     * @param keySpace key spacing character
     * @return the client builder instance
     */
    public final DynoClientBuilder withKeySpace(String keySpace) {
        this.keySpace = keySpace;
        return this;
    }

    /**
     * Set the buffer size
     *
     * @param bufferSize size of buffer
     * @return the client builder instance
     */
    public final DynoClientBuilder withBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }

    /**
     * Configure with bucket name
     *
     * @param bucketName name of S3 bucket
     * @return the client builder
     */
    public final DynoClientBuilder withBucket(String bucketName) {
        this.bucketName = bucketName;
        return this;
    }

    /**
     * Builds the Dyno with given parameters
     *
     * @return the Dyno instance
     */
    public final Dyno build() {
        return new Dyno(s3client, accessKey, secretKey, s3Endpoint, region, bucketName, keySpace, bufferSize);
    }

}
