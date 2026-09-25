package com.ailene.lms.common;

import java.security.SecureRandom;

public final class NanoId {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] ALPHABET = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            .toCharArray();
    private static final int SIZE = 21;

    private NanoId() {
    }

    // Same 64-char alphabet and length as the deployed nanoid() SQL function, so ids look alike either way.
    public static String generate() {
        char[] id = new char[SIZE];
        for (int i = 0; i < SIZE; i++) {
            id[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        }
        return new String(id);
    }
}
