package dev.vertesix.credifyqr.Identity.core.ports;

import dev.vertesix.credifyqr.Identity.core.domain.User;
import java.util.Optional;

public interface IdentityUseCase {
    Optional<User> authenticate(String username, String password);
    void registerUser(String username, String password, String role);
    Optional<User> findById(String id);
    String claimStudentAccount(String studentId, String birthdate);
    void changePassword(String userId, String newPassword);
}