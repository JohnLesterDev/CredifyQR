package dev.vertesix.credifyqr.Identity.core.domain;

public class User {
    private final String id;
    private final String username; // Student ID or Employee ID
    private String email;          // Staff Email (required for Staff claims)
    private String passwordHash;
    private final Role role;
    private final String birthdate; 
    private boolean isClaimed;
    private boolean needsPasswordReset;
    private boolean isActive;

    public User(
        String id, 
        String username, 
        String email,
        String passwordHash, 
        Role role, 
        String birthdate, 
        boolean isClaimed, 
        boolean needsPasswordReset,
        boolean isActive
        ) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.birthdate = birthdate;
        this.isClaimed = isClaimed;
        this.needsPasswordReset = needsPasswordReset;
        this.isActive = isActive;
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public String getBirthdate() { return birthdate; }
    public boolean isClaimed() { return isClaimed; }
    public void setClaimed(boolean claimed) { this.isClaimed = claimed; }
    public boolean needsPasswordReset() { return needsPasswordReset; }
    public void setNeedsPasswordReset(boolean needsPasswordReset) { this.needsPasswordReset = needsPasswordReset; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
    
    public boolean isTemporary() { return needsPasswordReset; }
    public boolean isSysAdmin() { return this.role == Role.SYSTEM_ADMIN; }
}