package dev.vertesix.credifyqr.Identity.core.service;

import dev.vertesix.credifyqr.Identity.core.domain.ActionType;
import dev.vertesix.credifyqr.Identity.core.domain.AuditLog;
import dev.vertesix.credifyqr.Identity.core.domain.Role;
import dev.vertesix.credifyqr.Identity.core.domain.User;
import dev.vertesix.credifyqr.Identity.core.ports.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.mindrot.jbcrypt.BCrypt;

// Instrumented IdentityService to hook AuditRepository into all authentication and mutation paths
public class IdentityService implements IdentityUseCase {

    private final UserRepository userRepository;
    private final SettingsRepository settingsRepository;
    private final PasswordGenerator passwordGenerator;
    private final AuditRepository auditRepository;

    private static final Pattern PASSWORD_POLICY = Pattern.compile("^(?=.*[a-zA-Z])(?=.*[0-9]).{12,}$");

    public IdentityService(UserRepository userRepository, SettingsRepository settingsRepository, PasswordGenerator passwordGenerator, AuditRepository auditRepository) {
        this.userRepository = userRepository;
        this.settingsRepository = settingsRepository;
        this.passwordGenerator = passwordGenerator;
        this.auditRepository = auditRepository;
    }

    private void logAudit(String actorId, ActionType type, String targetId, String ip) {
        auditRepository.save(new AuditLog(UUID.randomUUID().toString(), actorId, type, targetId, ip, System.currentTimeMillis()));
    }

    @Override
    public Optional<User> authenticate(String usernameOrEmail, String password, String ipAddress) {
        Optional<User> userOpt = userRepository.findByUsername(usernameOrEmail);
        
        if (userOpt.isEmpty() && usernameOrEmail.contains("@")) {
            userOpt = userRepository.findByEmail(usernameOrEmail);
        }

        if (userOpt.isEmpty()) {
            logAudit(usernameOrEmail, ActionType.LOGIN_FAILED, "UNKNOWN", ipAddress);
            return Optional.empty();
        }

        User user = userOpt.get();

        if (!user.isActive() || !user.isClaimed()) {
            logAudit(usernameOrEmail, ActionType.LOGIN_FAILED, user.getId(), ipAddress);
            return Optional.empty();
        }

        if (user.getRole() == Role.REGISTRAR_STAFF && !user.isApproved()) {
            logAudit(usernameOrEmail, ActionType.LOGIN_FAILED, user.getId(), ipAddress);
            return Optional.empty();
        }

        if (System.currentTimeMillis() < user.getLockoutUntil()) {
            logAudit(usernameOrEmail, ActionType.LOGIN_FAILED, user.getId(), ipAddress);
            long waitMinutes = (user.getLockoutUntil() - System.currentTimeMillis()) / 60000;
            if (waitMinutes < 1) waitMinutes = 1;
            throw new SecurityException("Account temporarily locked. Try again in " + waitMinutes + " minute(s).");
        }

        if (BCrypt.checkpw(password, user.getPasswordHash())) {
            if (user.getFailedLoginAttempts() > 0 || user.getLockoutUntil() > 0) {
                user.setFailedLoginAttempts(0);
                user.setLockoutUntil(0L);
                userRepository.save(user);
            }
            logAudit(user.getId(), ActionType.LOGIN_SUCCESS, user.getId(), ipAddress);
            return Optional.of(user);
        } else {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= 5) {
                user.setLockoutUntil(System.currentTimeMillis() + (15 * 60 * 1000));
                userRepository.save(user);
                logAudit(usernameOrEmail, ActionType.LOGIN_FAILED, user.getId(), ipAddress);
                throw new SecurityException("Account locked due to too many failed attempts. Try again in 15 minutes.");
            }
            userRepository.save(user);
            logAudit(usernameOrEmail, ActionType.LOGIN_FAILED, user.getId(), ipAddress);
            return Optional.empty();
        }
    }

    @Override
    public void registerUser(String username, String password, String roleStr, String ipAddress) {
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
            "", "", "", 
            true, false, true, true,
            0, 0L
        );
        userRepository.save(newUser);
        logAudit("SYSTEM", ActionType.USER_PROVISIONED, newUser.getId(), ipAddress);
    }

    @Override
    public Map<String, Object> provisionUser(String creatorId, String newUsername, String birthdate, Role targetRole, String firstName, String lastName, String middleInitial, String ipAddress) {
        User creator = userRepository.findById(creatorId)
            .orElseThrow(() -> new IllegalArgumentException("Creator not found."));

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
            false, 
            true,  
            true,  
            isApproved,
            0, 0L
        );
        
        userRepository.save(newUser);
        logAudit(creatorId, ActionType.USER_PROVISIONED, newUser.getId(), ipAddress);
        return Map.of("user", newUser);
    }

    @Override
    public String claimAccount(String identifier, String birthdate, String ipAddress) {
        User user = userRepository.findByUsername(identifier)
            .orElseThrow(() -> new IllegalArgumentException("Identity not found."));
        
        if (user.getRole() != Role.STUDENT) {
            throw new IllegalArgumentException("Staff must use the Secure Staff Claim portal.");
        }

        if (!birthdate.equals(user.getBirthdate())) {
            throw new IllegalArgumentException("Verification failed: Invalid details.");
        }

        String tempPwd = executeClaim(user);
        logAudit(user.getId(), ActionType.ACCOUNT_CLAIMED, user.getId(), ipAddress);
        return tempPwd;
    }

    @Override
    public String claimStaffAccount(String employeeId, String birthdate, String newEmail, String ipAddress) {
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

        // --- NEW FIX: Enforce Email Uniqueness ---
        Optional<User> existingUser = userRepository.findByEmail(newEmail);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
            throw new IllegalArgumentException("This email is already registered to another active account.");
        }
        // -----------------------------------------

        user.setEmail(newEmail);
        String tempPwd = executeClaim(user);
        logAudit(user.getId(), ActionType.ACCOUNT_CLAIMED, user.getId(), ipAddress);
        return tempPwd;
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
    public void changePassword(String userId, String newPassword, String ipAddress) {
        if (newPassword == null || !PASSWORD_POLICY.matcher(newPassword).matches()) {
            throw new IllegalArgumentException("Password must be at least 12 characters long and contain both letters and numbers.");
        }

        User user = userRepository.findById(userId).orElseThrow();
        user.setPasswordHash(hashPassword(newPassword));
        user.setNeedsPasswordReset(false);
        userRepository.save(user);
        logAudit(userId, ActionType.PASSWORD_CHANGED, userId, ipAddress);
    }

    @Override
    public void approveRegistrar(String directorId, String targetRegistrarId, String ipAddress) {
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
        logAudit(directorId, ActionType.ROLE_MODIFIED, targetRegistrarId, ipAddress);
    }

    @Override public Optional<User> findById(String id) { return userRepository.findById(id); }
    @Override public String getInstitutionDomain() { return settingsRepository.getSetting("INSTITUTION_DOMAIN").orElse(""); }
    
    @Override 
    public void updateInstitutionDomain(String directorId, String password, String newDomain, String ipAddress) {
        User admin = userRepository.findById(directorId).orElseThrow();
        if (admin.getRole() != Role.SYSTEM_ADMIN) throw new SecurityException("Only SysAdmins can modify the domain.");
        settingsRepository.saveSetting("INSTITUTION_DOMAIN", newDomain.replace("@", ""));
        logAudit(directorId, ActionType.DOMAIN_UPDATED, "SYSTEM", ipAddress);
    }

    @Override
    public List<User> getAllUsers(String requestingUserId) {
        User requester = userRepository.findById(requestingUserId).orElseThrow();
        if (requester.getRole() != Role.SYSTEM_ADMIN && requester.getRole() != Role.CAMPUS_DIRECTOR) {
            throw new SecurityException("Unauthorized to view system roster.");
        }
        return userRepository.findAll();
    }

    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }
}