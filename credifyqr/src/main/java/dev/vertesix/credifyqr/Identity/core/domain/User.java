package dev.vertesix.credifyqr.Identity.core.domain;

public class User {
    private final String id;
    private final String username;
    private String passwordHash;
    private final Role role;
    
    // Claiming workflow fields
    private final String birthdate; 
    private boolean isClaimed;
    private boolean needsPasswordReset;
    
    // Lifecycle state
    private boolean isActive;

    public User(
        String id, 
        String username, 
        String passwordHash, 
        Role role, 
        String birthdate, 
        boolean isClaimed, 
        boolean needsPasswordReset,
        boolean isActive
        ) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.birthdate = birthdate;
        this.isClaimed = isClaimed;
        this.needsPasswordReset = needsPasswordReset;
        this.isActive = isActive;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    
    public String getBirthdate() { return birthdate; }
    public boolean isClaimed() { return isClaimed; }
    public void setClaimed(boolean claimed) { this.isClaimed = claimed; }
    public boolean needsPasswordReset() { return needsPasswordReset; }
    public void setNeedsPasswordReset(boolean needsPasswordReset) { 
        this.needsPasswordReset = needsPasswordReset; 
    }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
    
    public boolean isTemporary() { return needsPasswordReset; }
    
    public boolean isDirector() {
        return this.role == Role.CAMPUS_DIRECTOR;
    }
}