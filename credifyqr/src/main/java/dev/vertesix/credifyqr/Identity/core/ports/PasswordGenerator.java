package dev.vertesix.credifyqr.Identity.core.ports;

public interface PasswordGenerator {
    String generate(int length);
}