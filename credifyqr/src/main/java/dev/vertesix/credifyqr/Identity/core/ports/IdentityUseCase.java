package dev.vertesix.credifyqr.Identity.core.ports;

import dev.vertesix.credifyqr.Identity.core.domain.Role;
import dev.vertesix.credifyqr.Identity.core.domain.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface IdentityUseCase {
    Optional<User> authenticate(String username, String password);
    void registerUser(String username, String password, String role);
    Map<String, Object> provisionUser(String creatorId, String newUsername, String birthdate, Role targetRole);
    Optional<User> findById(String id);
    
    // Student Pipeline (2-factor)
    String claimAccount(String identifier, String birthdate);
    
    // Staff Pipeline (3-factor) - NEW
    String claimStaffAccount(String employeeId, String email, String birthdate);
    
    void changePassword(String userId, String newPassword);
    String getInstitutionDomain();
    void updateInstitutionDomain(String directorId, String password, String newDomain);

    List<User> getAllUsers(String requestingUserId);
}