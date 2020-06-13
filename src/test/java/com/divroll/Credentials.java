package com.divroll;

public class Credentials {
    public static String getAccessKey() {
        return System.getenv("S3_ACCESS_KEY");
    }
    public static String getSecretKey() {
        return System.getenv("S3_SECRET_KEY");
    }
}
