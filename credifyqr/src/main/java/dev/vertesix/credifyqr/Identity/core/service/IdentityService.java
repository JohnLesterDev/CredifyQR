package dev.vertesix.credifyqr.Identity.core.service;

import dev.vertesix.credifyqr.Identity.core.domain.Role;
import dev.vertesix.credifyqr.Identity.core.domain.User;
import dev.vertesix.credifyqr.Identity.core.ports.UserRepository;
import dev.vertesix.credifyqr.Identity.core.ports.SettingsRepository;
import dev.vertesix.credifyqr.Identity.core.ports.IdentityUseCase;
import dev.vertesix.credifyqr.Identity.core.ports.PasswordGenerator;

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
            .filter(user -> !user.getPasswordHash().isEmpty())
            .filter(user -> BCrypt.checkpw(password, user.getPasswordHash()));
    }

    @Override
    public void registerUser(String username, String password, String roleStr) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists.");
        }
        Role role = Role.valueOf(roleStr.toUpperCase());
        String userId = UUID.randomUUID().toString();
        User newUser = new User(userId, username, hashPassword(password), role, "1990-01-01", true, false, true);
        userRepository.save(newUser);
    }

    @Override
    public Map<String, Object> provisionUser(String creatorId, String newUsername, String birthdate, Role targetRole) {
        User creator = userRepository.findById(creatorId)
            .orElseThrow(() -> new IllegalArgumentException("Creator not found."));

        if (!creator.isActive()) throw new SecurityException("Creator inactive.");
        if (creator.getRole() == Role.STUDENT) throw new SecurityException("Students cannot provision.");
        if (creator.getRole() == Role.CAMPUS_DIRECTOR && targetRole != Role.REGISTRAR_STAFF) throw new SecurityException("Directors can only provision Registrar Staff.");
        if (creator.getRole() == Role.REGISTRAR_STAFF && targetRole != Role.STUDENT) throw new SecurityException("Registrar can only provision Students.");

        String finalUsername = newUsername;

        if (targetRole == Role.REGISTRAR_STAFF) {
            String domain = getInstitutionDomain();
            if (domain == null || domain.isBlank()) {
                throw new IllegalStateException("Institution domain must be set before provisioning staff.");
            }
            if (newUsername.contains("@")) {
                throw new IllegalArgumentException("Enter only the username prefix. The domain is appended automatically.");
            }
            finalUsername = newUsername + "@" + domain;
        }

        if (userRepository.findByUsername(finalUsername).isPresent()) {
            throw new IllegalArgumentException("Username/ID already exists.");
        }

        User newUser = new User(
            UUID.randomUUID().toString(), 
            finalUsername, 
            "", 
            targetRole, 
            birthdate, 
            false, 
            true,  
            true   
        );
        
        userRepository.save(newUser);
        
        return Map.of("user", newUser, "tempPassword", "");
    }

    @Override
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    @Override
    public String claimAccount(String identifier, String birthdate) {
        User user = userRepository.findByUsername(identifier)
            .orElseThrow(() -> new IllegalArgumentException("Identity not found in the system."));

        if (!user.isActive()) throw new IllegalArgumentException("Account is suspended.");
        if (user.isClaimed()) throw new IllegalArgumentException("Account already claimed. Proceed to login.");
        if (!user.getBirthdate().equals(birthdate)) throw new IllegalArgumentException("Invalid birthdate.");

        String tempPassword = passwordGenerator.generate(12);
        
        user.setPasswordHash(hashPassword(tempPassword));
        user.setClaimed(true);
        user.setNeedsPasswordReset(true);
        
        userRepository.save(user);
        return tempPassword;
    }

    @Override
    public void changePassword(String userId, String newPassword) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found."));
        user.setPasswordHash(hashPassword(newPassword));
        user.setNeedsPasswordReset(false);
        userRepository.save(user);
    }

    @Override
    public String getInstitutionDomain() {
        return settingsRepository.getSetting("INSTITUTION_DOMAIN").orElse(null);
    }

    @Override
    public void updateInstitutionDomain(String directorId, String password, String newDomain) {
        User director = userRepository.findById(directorId).orElseThrow();
        if (director.getRole() != Role.CAMPUS_DIRECTOR) throw new SecurityException("Only Campus Directors can alter the domain.");
        if (!BCrypt.checkpw(password, director.getPasswordHash())) throw new SecurityException("Authentication failed.");
        
        newDomain = newDomain.replace("@", "");
        if (newDomain.isBlank()) throw new IllegalArgumentException("Domain cannot be empty.");
        
        settingsRepository.saveSetting("INSTITUTION_DOMAIN", newDomain);
    }

    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }
}