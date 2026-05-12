package dev.vertesix.credifyqr.Identity.core.ports;

import dev.vertesix.credifyqr.Identity.core.domain.Role;
import dev.vertesix.credifyqr.Identity.core.domain.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface IdentityUseCase {
    Optional<User> authenticate(String username, String password);
    void registerUser(String username, String password, String role);
    
    // Provisioning updated with name fields
    Map<String, Object> provisionUser(String creatorId, String newUsername, String birthdate, Role targetRole, String firstName, String lastName, String middleInitial);
    
    Optional<User> findById(String id);
    
    // Student Pipeline
    String claimAccount(String identifier, String birthdate);
    
    // Employee Pipeline (ID + BDay -> Set Work Email -> Temp Password)
    String claimStaffAccount(String employeeId, String birthdate, String newEmail);
    
    void changePassword(String userId, String newPassword);
    String getInstitutionDomain();
    void updateInstitutionDomain(String directorId, String password, String newDomain);

    List<User> getAllUsers(String requestingUserId);
    
    // Role-specific Approval Pipeline
    void approveRegistrar(String directorId, String targetRegistrarId);
}