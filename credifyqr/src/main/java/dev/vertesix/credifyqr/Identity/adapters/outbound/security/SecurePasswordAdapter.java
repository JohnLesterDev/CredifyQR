package dev.vertesix.credifyqr.Identity.adapters.outbound.security;

import java.security.SecureRandom;

import dev.vertesix.credifyqr.Identity.core.ports.PasswordGenerator;

public class SecurePasswordAdapter implements PasswordGenerator {
    private final SecureRandom random = new SecureRandom();
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    @Override
    public String generate(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}