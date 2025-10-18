package com.example.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CryptoUtil {
    public static MessageDigest md;

    static {
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private CryptoUtil() {
        // Private constructor to prevent instantiation
    }

    public static String toMD5(String value) {
        var sb = new StringBuilder();
        for (var b : md.digest(value.getBytes())) {
            sb.append(String.format("%02x", b & 0xff));
        }

        return sb.toString();
    }
}
