package com.example.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Encryptor {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final byte[] IV = new byte[16]; // Initialization vector (16 bytes for AES)
    private static final int KEY_LENGTH = 32; // 256 bits for AES-256

    private Encryptor() {
        // Private constructor to prevent instantiation
    }

    // Derive a fixed-length key from an arbitrary-length secret using SHA-256
    static byte[] deriveKey(String secretKey) throws Exception {
        var sha256 = MessageDigest.getInstance("SHA-256");
        var keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        var hashedKey = sha256.digest(keyBytes);

        // Truncate or use as-is to match desired key length (32 bytes for AES-256)
        var finalKey = new byte[KEY_LENGTH];
        System.arraycopy(hashedKey, 0, finalKey, 0, Math.min(hashedKey.length, KEY_LENGTH));

        return finalKey;
    }

    public static String encrypt(String input, String secretKey) throws Exception {
        var keyBytes = deriveKey(secretKey);
        var secretKeySpec = new SecretKeySpec(keyBytes, ALGORITHM);
        var ivParameterSpec = new IvParameterSpec(IV);

        var cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

        var encrypted = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));

        return Base64
                .getEncoder()
                .encodeToString(encrypted);
    }

    public static String decrypt(String encryptedInput, String secretKey) throws Exception {
        var keyBytes = deriveKey(secretKey);
        var secretKeySpec = new SecretKeySpec(keyBytes, ALGORITHM);
        var ivParameterSpec = new IvParameterSpec(IV);

        var cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

        var decoded = Base64.getDecoder().decode(encryptedInput);
        var decrypted = cipher.doFinal(decoded);

        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
