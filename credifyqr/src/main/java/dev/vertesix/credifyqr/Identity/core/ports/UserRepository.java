package dev.vertesix.credifyqr.Identity.core.ports;

import dev.vertesix.credifyqr.Identity.core.domain.User;
import java.util.Optional;

/**
 * Outbound Port for User Persistence.
 * This interface decouples the core logic from specific database implementations.
 */
public interface UserRepository {
    Optional<User> findByUsername(String username);
    Optional<User> findById(String id);
    void save(User user);
}
