package dev.vertesix.credifyqr.Identity.core.ports;

import dev.vertesix.credifyqr.Identity.core.domain.Role;
import dev.vertesix.credifyqr.Identity.core.domain.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

// Mutated interface to require network context (IP) for compliant audit trails
public interface IdentityUseCase {
    Optional<User> authenticate(String username, String password, String ipAddress);
    void registerUser(String username, String password, String role, String ipAddress);
    
    Map<String, Object> provisionUser(String creatorId, String newUsername, String birthdate, Role targetRole, String firstName, String lastName, String middleInitial, String ipAddress);
    
    Optional<User> findById(String id);
    
    String claimAccount(String identifier, String birthdate, String ipAddress);
    
    String claimStaffAccount(String employeeId, String birthdate, String newEmail, String ipAddress);
    
    void changePassword(String userId, String newPassword, String ipAddress);
    String getInstitutionDomain();
    void updateInstitutionDomain(String directorId, String password, String newDomain, String ipAddress);

    List<User> getAllUsers(String requestingUserId);
    
    void approveRegistrar(String directorId, String targetRegistrarId, String ipAddress);
}