package dev.vertesix.credifyqr.Identity.core.ports;

import dev.vertesix.credifyqr.Identity.core.domain.User;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for user identity persistence and lookup operations.
 */
public interface UserRepository {
    /**
     * Finds a user by username.
     */
    Optional<User> findByUsername(String username);
    /**
     * Finds a user by unique id.
     */
    Optional<User> findById(String id);
    /**
     * Finds a user by email address.
     */
    Optional<User> findByEmail(String email); // New lookup port
    List<User> findAll();   
    void save(User user);
}