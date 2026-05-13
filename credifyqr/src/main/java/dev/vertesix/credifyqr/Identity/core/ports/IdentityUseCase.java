package dev.vertesix.credifyqr.Identity.core.ports;

import dev.vertesix.credifyqr.Identity.core.domain.Role;
import dev.vertesix.credifyqr.Identity.core.domain.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Use case boundary for identity and authentication operations.
 *
 * <p>This interface defines the key operations available to the identity management
 * domain from the web and application layers.</p>
 */
public interface IdentityUseCase {
    /**
     * Authenticates a user with credentials and audit context.
     *
     * @param username username or email to authenticate
     * @param password plaintext password
     * @param ipAddress source IP address for audit logging
     * @return optional authenticated user
     */
    Optional<User> authenticate(String username, String password, String ipAddress);
    /**
     * Registers a new user account.
     *
     * @param username chosen username
     * @param password chosen password
     * @param role user role string
     * @param ipAddress source IP address for audit logging
     */
    void registerUser(String username, String password, String role, String ipAddress);
    /**
     * Provisions a new user account with the specified role and profile data.
     *
     * @param creatorId id of the user creating the account
     * @param newUsername requested username
     * @param birthdate birthdate for verification
     * @param targetRole role to assign
     * @param firstName first name of the new user
     * @param lastName last name of the new user
     * @param middleInitial middle initial of the new user
     * @param ipAddress source IP address for audit logging
     * @return a map containing the provisioned user details
     */
    Map<String, Object> provisionUser(String creatorId, String newUsername, String birthdate, Role targetRole, String firstName, String lastName, String middleInitial, String ipAddress);
    /**
     * Finds a user by unique identifier.
     *
     * @param id user identifier
     * @return optional user if found
     */
    Optional<User> findById(String id);
    /**
     * Claims a student account by verifying identity data.
     *
     * @param identifier student identifier
     * @param birthdate birthdate for verification
     * @param ipAddress source IP address for audit logging
     * @return temporary password for the claimed account
     */
    String claimAccount(String identifier, String birthdate, String ipAddress);
    /**
     * Claims an employee account and binds it to a work email.
     *
     * @param employeeId employee identifier
     * @param birthdate birthdate for verification
     * @param newEmail work email address
     * @param ipAddress source IP address for audit logging
     * @return temporary password for the claimed account
     */
    String claimStaffAccount(String employeeId, String birthdate, String newEmail, String ipAddress);
    /**
     * Changes a user's password and resets temporary login state.
     *
     * @param userId user identifier
     * @param newPassword new plaintext password
     * @param ipAddress source IP address for audit logging
     */
    void changePassword(String userId, String newPassword, String ipAddress);
    /**
     * Returns the configured institution domain.
     *
     * @return institution domain string
     */
    String getInstitutionDomain();
    /**
     * Updates the institution domain setting after validating administrative credentials.
     *
     * @param directorId id of the approving director
     * @param password director password for validation
     * @param newDomain new institution domain
     * @param ipAddress source IP address for audit logging
     */
    void updateInstitutionDomain(String directorId, String password, String newDomain, String ipAddress);
    /**
     * Returns all users visible to the requesting administrator.
     *
     * @param requestingUserId id of the requesting administrator
     * @return list of users
     */
    List<User> getAllUsers(String requestingUserId);
    /**
     * Approves a registrar account.
     *
     * @param directorId id of the campus director
     * @param targetRegistrarId id of the registrar to approve
     * @param ipAddress source IP address for audit logging
     */
    void approveRegistrar(String directorId, String targetRegistrarId, String ipAddress);

    /**
     * Disables an active registrar account by revoking approval.
     *
     * @param adminId id of the system administrator
     * @param targetRegistrarId id of the registrar to disable
     * @param ipAddress source IP address for audit logging
     */
    void disableRegistrar(String adminId, String targetRegistrarId, String ipAddress);
}