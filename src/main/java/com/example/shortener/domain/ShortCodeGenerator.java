package com.example.shortener.domain;

import java.security.SecureRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ShortCodeGenerator implements AliasGenerator {

    private static final char[] ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private final SecureRandom random = new SecureRandom();
    private final int length;

    public ShortCodeGenerator(@Value("${app.short-code-length:7}") int length) {
        this.length = length;
    }

    @Override
    public String generate() {
        char[] output = new char[length];
        for (int index = 0; index < length; index++) {
            output[index] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(output);
    }
}
