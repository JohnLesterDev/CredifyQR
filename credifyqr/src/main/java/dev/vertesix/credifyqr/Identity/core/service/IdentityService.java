package dev.vertesix.credifyqr.Identity.core.service;

import dev.vertesix.credifyqr.Identity.core.domain.Role;
import dev.vertesix.credifyqr.Identity.core.domain.User;
import dev.vertesix.credifyqr.Identity.core.ports.IdentityUseCase;
import dev.vertesix.credifyqr.Identity.core.ports.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;

public class IdentityService implements IdentityUseCase {

    private final UserRepository userRepository;

    // Dependency Injection: We pass the port (interface), not the SQLite adapter.
    public IdentityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<User> authenticate(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String hashedInput = hashPassword(password);
            
            // If the hashes match, authentication is successful
            if (user.getPasswordHash().equals(hashedInput)) {
                return Optional.of(user);
            }
        }
        return Optional.empty(); // Login failed
    }

    @Override
    public void registerUser(String username, String password, String roleStr) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists.");
        }

        Role role;
        try {
            role = Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role. Must be STUDENT, REGISTRAR_STAFF, or CAMPUS_DIRECTOR.");
        }

        String userId = UUID.randomUUID().toString();
        String passwordHash = hashPassword(password);

        User newUser = new User(userId, username, passwordHash, role);
        userRepository.save(newUser);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash password.", e);
        }
    }

    @Override
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }
}
