package dev.vertesix.credifyqr.Identity.core.ports;

public interface TokenBlacklistRepository {
    void blacklist(String jti, long expiresAtMillis);
    boolean isBlacklisted(String jti);
}