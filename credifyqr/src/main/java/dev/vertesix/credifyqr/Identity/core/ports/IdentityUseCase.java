package dev.vertesix.credifyqr.Identity.core.ports;

import dev.vertesix.credifyqr.Identity.core.domain.Role;
import dev.vertesix.credifyqr.Identity.core.domain.User;
import java.util.Map;
import java.util.Optional;

public interface IdentityUseCase {
    Optional<User> authenticate(String username, String password);
    void registerUser(String username, String password, String role);
    
    Map<String, Object> provisionUser(String creatorId, String newUsername, String birthdate, Role targetRole);
    
    Optional<User> findById(String id);
    
    // Claiming now handles both Students and Staff
    String claimAccount(String identifier, String birthdate);
    void changePassword(String userId, String newPassword);

    // New Domain Management
    String getInstitutionDomain();
    void updateInstitutionDomain(String directorId, String password, String newDomain);
}