package dev.vertesix.credifyqr.Identity.core.service;

import dev.vertesix.credifyqr.Identity.core.domain.Role;
import dev.vertesix.credifyqr.Identity.core.domain.User;
import dev.vertesix.credifyqr.Identity.core.ports.UserRepository;
import dev.vertesix.credifyqr.Identity.core.ports.IdentityUseCase;
import dev.vertesix.credifyqr.Identity.core.ports.PasswordGenerator;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Optional;
import java.util.UUID;

import org.mindrot.jbcrypt.BCrypt;



public class IdentityService implements IdentityUseCase {

    private final UserRepository userRepository;
    private final PasswordGenerator passwordGenerator;

    public IdentityService(UserRepository userRepository, PasswordGenerator passwordGenerator) {
        this.userRepository = userRepository;
        this.passwordGenerator = passwordGenerator;
    }

    @Override
    public Optional<User> authenticate(String username, String password) {
        return userRepository.findByUsername(username)
            .filter(User::isClaimed) 
            .filter(user -> !user.getPasswordHash().isEmpty())
            .filter(user -> BCrypt.checkpw(password, user.getPasswordHash()));
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

        User newUser = new User(userId, username, passwordHash, role, "1990-01-01", true, false);
        userRepository.save(newUser);
    }

    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    @Override
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    @Override
    public String claimStudentAccount(String studentId, String birthdate) {
        User user = userRepository.findByUsername(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student ID not found in the system."));

        if (user.isClaimed()) {
            throw new IllegalArgumentException("Account has already been claimed. Proceed to login.");
        }

        if (!user.getBirthdate().equals(birthdate)) {
            throw new IllegalArgumentException("Verification failed. Invalid birthdate.");
        }

        String tempPassword = passwordGenerator.generate(12);
        
        user.setPasswordHash(hashPassword(tempPassword));
        user.setClaimed(true);
        user.setNeedsPasswordReset(true);
        
        userRepository.save(user);

        return tempPassword;
    }

    @Override
    public void changePassword(String userId, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found."));

        user.setPasswordHash(hashPassword(newPassword));
        user.setNeedsPasswordReset(false);
        
        userRepository.save(user);
    }
}