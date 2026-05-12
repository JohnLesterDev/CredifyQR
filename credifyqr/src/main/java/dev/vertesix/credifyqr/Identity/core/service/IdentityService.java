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
    public Optional<User> authenticate(String usernameOrEmail, String password) {
        Optional<User> userOpt = userRepository.findByUsername(usernameOrEmail);
        
        if (userOpt.isEmpty() && usernameOrEmail.contains("@")) {
            userOpt = userRepository.findByEmail(usernameOrEmail);
        }

        return userOpt
            .filter(User::isActive)
            .filter(User::isClaimed)
            .filter(user -> {
                // Registrar accounts require explicit Campus Director approval
                if (user.getRole() == Role.REGISTRAR_STAFF) return user.isApproved();
                return true;
            })
            .filter(user -> BCrypt.checkpw(password, user.getPasswordHash()));
    }

    @Override
    public void registerUser(String username, String password, String roleStr) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists.");
        }
        Role role = Role.valueOf(roleStr.toUpperCase());
        User newUser = new User(
            UUID.randomUUID().toString(), 
            username, 
            null, 
            hashPassword(password), 
            role, 
            "1990-01-01", 
            "", "", "", // Blank names for legacy registration
            true, false, true, true 
        );
        userRepository.save(newUser);
    }

    @Override
    public Map<String, Object> provisionUser(String creatorId, String newUsername, String birthdate, Role targetRole, String firstName, String lastName, String middleInitial) {
        User creator = userRepository.findById(creatorId)
            .orElseThrow(() -> new IllegalArgumentException("Creator not found."));

        // Strict boundary enforcement
        if (targetRole == Role.CAMPUS_DIRECTOR || targetRole == Role.REGISTRAR_STAFF) {
            if (creator.getRole() != Role.SYSTEM_ADMIN) {
                throw new SecurityException("Only System Administrators can provision Employee accounts.");
            }
        } else if (targetRole == Role.STUDENT) {
            if (creator.getRole() != Role.REGISTRAR_STAFF) {
                throw new SecurityException("Only Registrar Staff can provision Student accounts.");
            }
        }

        if (userRepository.findByUsername(newUsername).isPresent()) {
            throw new IllegalArgumentException("Identifier already exists.");
        }

        // Registrars are locked (isApproved = false) until Campus Director intervenes
        boolean isApproved = (targetRole != Role.REGISTRAR_STAFF);

        User newUser = new User(
            UUID.randomUUID().toString(),
            newUsername,
            null, 
            "",   
            targetRole,
            birthdate,
            firstName,
            lastName,
            middleInitial,
            false, // isClaimed
            true,  // needsPasswordReset
            true,  // isActive
            isApproved
        );
        
        userRepository.save(newUser);
        return Map.of("user", newUser);
    }

    @Override
    public String claimAccount(String identifier, String birthdate) {
        User user = userRepository.findByUsername(identifier)
            .orElseThrow(() -> new IllegalArgumentException("Identity not found."));
        
        if (user.getRole() != Role.STUDENT) {
            throw new IllegalArgumentException("Staff must use the Secure Staff Claim portal.");
        }

        if (!birthdate.equals(user.getBirthdate())) {
            throw new IllegalArgumentException("Verification failed: Invalid details.");
        }

        return executeClaim(user);
    }

    @Override
    public String claimStaffAccount(String employeeId, String birthdate, String newEmail) {
        User user = userRepository.findByUsername(employeeId)
            .orElseThrow(() -> new IllegalArgumentException("Staff identity not found."));

        if (user.getRole() == Role.STUDENT) {
            throw new IllegalArgumentException("Invalid portal for Student identity.");
        }

        if (!birthdate.equals(user.getBirthdate())) {
            throw new IllegalArgumentException("Verification failed: Invalid details.");
        }

        if (newEmail == null || newEmail.isBlank()) {
            throw new IllegalArgumentException("Work email is required to claim an Employee account.");
        }

        // Set the requested work email natively during the claim process
        user.setEmail(newEmail);

        return executeClaim(user);
    }

    private String executeClaim(User user) {
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

    @Override
    public void approveRegistrar(String directorId, String targetRegistrarId) {
        User director = userRepository.findById(directorId).orElseThrow();
        if (director.getRole() != Role.CAMPUS_DIRECTOR) {
            throw new SecurityException("Only Campus Directors can approve Registrars.");
        }
        
        User registrar = userRepository.findById(targetRegistrarId)
            .orElseThrow(() -> new IllegalArgumentException("Target user not found."));
            
        if (registrar.getRole() != Role.REGISTRAR_STAFF) {
            throw new IllegalArgumentException("Target is not a Registrar account.");
        }
        
        registrar.setApproved(true);
        userRepository.save(registrar);
    }

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
        // Allow SysAdmin OR Campus Director to pull lists (CD needs it to see pending Registrars)
        if (requester.getRole() != Role.SYSTEM_ADMIN && requester.getRole() != Role.CAMPUS_DIRECTOR) {
            throw new SecurityException("Unauthorized to view system roster.");
        }
        return userRepository.findAll();
    }

    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }
}