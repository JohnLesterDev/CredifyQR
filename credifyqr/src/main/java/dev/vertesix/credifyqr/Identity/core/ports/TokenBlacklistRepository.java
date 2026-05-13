package dev.vertesix.credifyqr.Identity.core.ports;

/**
 * Port for managing blacklisted JWT token identifiers.
 */
public interface TokenBlacklistRepository {
    /**
     * Marks a token identifier as revoked until its expiration time.
     */
    void blacklist(String jti, long expiresAtMillis);
    /**
     * Returns true when the token identifier is revoked.
     */
    boolean isBlacklisted(String jti);
}