package ru.qa.endpoint.data;

import java.security.SecureRandom;

public final class Tokens {

    public static final int LENGTH = 32;
    public static final String DIGITS = "0123456789";
    public static final String HEX_LETTERS = "ABCDEF";
    public static final String NON_HEX_LETTERS = "GHIJKLMNOPQRSTUVWXYZ";
    public static final String ALL_ALLOWED = HEX_LETTERS + NON_HEX_LETTERS + DIGITS;

    private static final SecureRandom RANDOM = new SecureRandom();

    private Tokens() {
    }

    public static String random() {
        return random(DIGITS + HEX_LETTERS);
    }

    public static String random(String alphabet) {
        return random(alphabet, LENGTH);
    }

    public static String random(String alphabet, int length) {
        StringBuilder token = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            token.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return token.toString();
    }

    public static String withNonHexLetter() {
        String token = random(ALL_ALLOWED, LENGTH - 1);
        return token + NON_HEX_LETTERS.charAt(RANDOM.nextInt(NON_HEX_LETTERS.length()));
    }
}
