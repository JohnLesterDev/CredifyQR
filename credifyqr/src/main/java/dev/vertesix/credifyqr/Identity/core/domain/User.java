package dev.vertesix.credifyqr.Identity.core.domain;

public class User {
    private final String id;
    private final String username;
    private String passwordHash;
    private final Role role;

    public User(String id, String username, String passwordHash, Role role) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    
    // Domain logic enforcing the authority rule
    public boolean isDirector() {
        return this.role == Role.CAMPUS_DIRECTOR;
    }
}
