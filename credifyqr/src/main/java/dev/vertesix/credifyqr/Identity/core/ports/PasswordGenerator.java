package dev.vertesix.credifyqr.Identity.core.ports;

/**
 * Port for generating secure passwords.
 */
public interface PasswordGenerator {
    /**
     * Generates a random password of the requested length.
     */
    String generate(int length);
}