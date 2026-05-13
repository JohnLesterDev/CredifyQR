package dev.vertesix.credifyqr.Identity.core.domain;

/**
 * Domain entity representing an application user.
 *
 * <p>Encapsulates identity attributes, account state, and role information.</p>
 */
public class User {
    private final String id;
    private final String username;
    private String email;
    private String passwordHash;
    private final Role role;
    private final String birthdate;
    
    private String firstName;
    private String lastName;
    private String middleInitial;
    
    private boolean isClaimed;
    private boolean needsPasswordReset;
    private boolean isActive;
    private boolean isApproved;

    // Changes for rate-limiting implementation: tracking brute-force vectors
    private int failedLoginAttempts;
    private long lockoutUntil;

    public User(
        String id, 
        String username, 
        String email,
        String passwordHash, 
        Role role, 
        String birthdate, 
        String firstName,
        String lastName,
        String middleInitial,
        boolean isClaimed, 
        boolean needsPasswordReset,
        boolean isActive,
        boolean isApproved,
        int failedLoginAttempts,
        long lockoutUntil
        ) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.birthdate = birthdate;
        this.firstName = firstName;
        this.lastName = lastName;
        this.middleInitial = middleInitial;
        this.isClaimed = isClaimed;
        this.needsPasswordReset = needsPasswordReset;
        this.isActive = isActive;
        this.isApproved = isApproved;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockoutUntil = lockoutUntil;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public String getBirthdate() { return birthdate; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getMiddleInitial() { return middleInitial; }
    public void setMiddleInitial(String middleInitial) { this.middleInitial = middleInitial; }

    public String getFullName() {
        if ((firstName == null || firstName.isBlank()) && (lastName == null || lastName.isBlank())) {
            return username;
        }
        String mi = (middleInitial != null && !middleInitial.isBlank()) ? " " + middleInitial + "." : "";
        return (lastName != null ? lastName + ", " : "") + (firstName != null ? firstName : "") + mi;
    }

    public boolean isClaimed() { return isClaimed; }
    public void setClaimed(boolean claimed) { this.isClaimed = claimed; }
    public boolean needsPasswordReset() { return needsPasswordReset; }
    public void setNeedsPasswordReset(boolean needsPasswordReset) { this.needsPasswordReset = needsPasswordReset; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
    
    public boolean isApproved() { return isApproved; }
    public void setApproved(boolean approved) { this.isApproved = approved; }
    
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }
    public long getLockoutUntil() { return lockoutUntil; }
    public void setLockoutUntil(long lockoutUntil) { this.lockoutUntil = lockoutUntil; }
    
    public boolean isTemporary() { return needsPasswordReset; }
    public boolean isSysAdmin() { return this.role == Role.SYSTEM_ADMIN; }
}