package dev.vertesix.credifyqr.Identity.core.domain;

public class User {
    private final String id;
    private final String username; // Student ID or Employee ID
    private String email;          // Staff Email (required for Staff claims)
    private String passwordHash;
    private final Role role;
    private final String birthdate; 
    
    // New fields for Task 2
    private String firstName;
    private String lastName;
    private String middleInitial;
    
    private boolean isClaimed;
    private boolean needsPasswordReset;
    private boolean isActive;
    
    // New field for Task 5
    private boolean isApproved;

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
        boolean isApproved
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
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getMiddleInitial() { return middleInitial; }
    public void setMiddleInitial(String middleInitial) { this.middleInitial = middleInitial; }

    public String getFullName() {
        if ((firstName == null || firstName.isBlank()) && (lastName == null || lastName.isBlank())) {
            return username; // Fallback for legacy seeded accounts
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
    
    public boolean isTemporary() { return needsPasswordReset; }
    public boolean isSysAdmin() { return this.role == Role.SYSTEM_ADMIN; }
}