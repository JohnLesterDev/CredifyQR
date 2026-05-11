package dev.vertesix.credifyqr.Identity.core.service;

import dev.vertesix.credifyqr.Identity.core.domain.Role;
import dev.vertesix.credifyqr.Identity.core.domain.User;
import dev.vertesix.credifyqr.Identity.core.ports.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.mindrot.jbcrypt.BCrypt;

public class IdentityService implements IdentityUseCase {

    private final UserRepository userRepository;
    private final SettingsRepository settingsRepository;
    private final PasswordGenerator passwordGenerator;

    public IdentityService(UserRepository userRepository, SettingsRepository settingsRepository, PasswordGenerator passwordGenerator) {
        this.userRepository = userRepository;
        this.settingsRepository = settingsRepository;
        this.passwordGenerator = passwordGenerator;
    }

    @Override
    public Optional<User> authenticate(String username, String password) {
        return userRepository.findByUsername(username)
            .filter(User::isActive)
            .filter(User::isClaimed) 
            .filter(user -> BCrypt.checkpw(password, user.getPasswordHash()));
    }

    @Override
    public void registerUser(String username, String password, String roleStr) {
        // This is primarily used for seeding or open registration if enabled
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists.");
        }
        Role role = Role.valueOf(roleStr.toUpperCase());
        User newUser = new User(
            UUID.randomUUID().toString(), 
            username, 
            null, // Email usually provided later or via provisioning
            hashPassword(password), 
            role, 
            "1990-01-01", true, false, true
        );
        userRepository.save(newUser);
    }

    @Override
    public Map<String, Object> provisionUser(String creatorId, String newUsername, String birthdate, Role targetRole) {
        User creator = userRepository.findById(creatorId)
            .orElseThrow(() -> new IllegalArgumentException("Creator not found."));

        // Logic Overhaul: SysAdmin manages Staff. Registrar manages Students.
        if (targetRole == Role.CAMPUS_DIRECTOR || targetRole == Role.REGISTRAR_STAFF) {
            if (creator.getRole() != Role.SYSTEM_ADMIN) {
                throw new SecurityException("Only System Administrators can provision Staff accounts.");
            }
        } else if (targetRole == Role.STUDENT) {
            if (creator.getRole() != Role.REGISTRAR_STAFF && creator.getRole() != Role.SYSTEM_ADMIN) {
                throw new SecurityException("Insufficient permissions to provision Student accounts.");
            }
        }

        if (userRepository.findByUsername(newUsername).isPresent()) {
            throw new IllegalArgumentException("Identifier already exists.");
        }

        // Create in 'Unclaimed' state
        User newUser = new User(
            UUID.randomUUID().toString(),
            newUsername,
            null, // Email will be set during claim or by SysAdmin later
            "",   // No password until claimed
            targetRole,
            birthdate,
            false, // isClaimed
            true,  // needsPasswordReset
            true   // isActive
        );
        
        userRepository.save(newUser);
        return Map.of("user", newUser);
    }

    @Override
    public String claimAccount(String identifier, String birthdate) {
        // Default claim for Students (ID + DOB)
        User user = userRepository.findByUsername(identifier)
            .orElseThrow(() -> new IllegalArgumentException("Identity not found."));
        
        if (user.getRole() != Role.STUDENT) {
            throw new IllegalArgumentException("Staff must use the Secure Staff Claim portal.");
        }

        return executeClaim(user, birthdate);
    }

    // New Secure Pipeline for Staff: ID + Email + DOB
    public String claimStaffAccount(String employeeId, String email, String birthdate) {
        User user = userRepository.findByUsername(employeeId)
            .orElseThrow(() -> new IllegalArgumentException("Staff identity not found."));

        if (user.getRole() == Role.STUDENT) {
            throw new IllegalArgumentException("Invalid portal for Student identity.");
        }

        // Match all three factors
        if (!birthdate.equals(user.getBirthdate())) {
            throw new IllegalArgumentException("Verification failed: Invalid details.");
        }

        // Link email to account if not already set, then verify
        if (user.getEmail() == null) {
            user.setEmail(email);
        } else if (!user.getEmail().equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("Verification failed: Email mismatch.");
        }

        return executeClaim(user, birthdate);
    }

    private String executeClaim(User user, String birthdate) {
        if (user.isClaimed()) throw new IllegalArgumentException("Account already claimed.");
        if (!user.isActive()) throw new IllegalArgumentException("Account suspended.");

        String tempPassword = passwordGenerator.generate(12);
        user.setPasswordHash(hashPassword(tempPassword));
        user.setClaimed(true);
        user.setNeedsPasswordReset(true);
        
        userRepository.save(user);
        return tempPassword;
    }

    @Override
    public void changePassword(String userId, String newPassword) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setPasswordHash(hashPassword(newPassword));
        user.setNeedsPasswordReset(false);
        userRepository.save(user);
    }

    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    // Port implementations for domain and finding users...
    @Override public Optional<User> findById(String id) { return userRepository.findById(id); }
    @Override public String getInstitutionDomain() { return settingsRepository.getSetting("INSTITUTION_DOMAIN").orElse(""); }
    @Override public void updateInstitutionDomain(String directorId, String password, String newDomain) {
        User admin = userRepository.findById(directorId).orElseThrow();
        if (admin.getRole() != Role.SYSTEM_ADMIN) throw new SecurityException("Only SysAdmins can modify the domain.");
        settingsRepository.saveSetting("INSTITUTION_DOMAIN", newDomain.replace("@", ""));
    }

    @Override
    public List<User> getAllUsers(String requestingUserId) {
        User requester = userRepository.findById(requestingUserId).orElseThrow();
        if (requester.getRole() != Role.SYSTEM_ADMIN) {
            throw new SecurityException("Only System Administrators can view the user roster.");
        }
        return userRepository.findAll();
    }
}   