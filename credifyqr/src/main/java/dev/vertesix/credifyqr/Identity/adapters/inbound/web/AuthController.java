package dev.vertesix.credifyqr.Identity.adapters.inbound.web;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.vertesix.credifyqr.Identity.core.domain.Role;
import dev.vertesix.credifyqr.Identity.core.domain.User;
import dev.vertesix.credifyqr.Identity.core.ports.IdentityUseCase;
import dev.vertesix.credifyqr.Identity.core.ports.TokenBlacklistRepository;
import dev.vertesix.credifyqr.Identity.core.security.SanitizerUtil;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.SameSite;
import io.jsonwebtoken.Claims;

public class AuthController {

    private final IdentityUseCase identityUseCase;
    private final TokenBlacklistRepository blacklistRepository;

    public AuthController(IdentityUseCase identityUseCase, TokenBlacklistRepository blacklistRepository) {
        this.identityUseCase = identityUseCase;
        this.blacklistRepository = blacklistRepository;
    }

    public void registerRoutes(Javalin app) {
        app.post("/api/login", this::login);
        app.post("/api/logout", this::logout);
        app.get("/api/session", this::session);
        
        app.post("/api/claim-account", this::claimAccount);
        app.post("/api/staff/claim-account", this::claimStaffAccount);
        
        app.post("/api/change-password", this::changePassword);
        
        app.post("/api/admin/users", this::provisionUser);
        app.post("/api/admin/users/approve", this::approveRegistrar);
        
        // ADD THIS: Public read-only route for the login page
        app.get("/api/public/domain", this::getDomain);
        
        // Keep these for the authenticated SysAdmin dashboard
        app.get("/api/admin/settings/domain", this::getDomain);
        app.post("/api/admin/settings/domain", this::setDomain);

        app.get("/api/admin/users", this::getAllUsers);
        app.post("/api/admin/clear-cache", this::clearCache);
    }

    private void login(Context ctx) {
        // Drop SanitizerUtil so the '@' symbol in emails survives
        String usernameOrEmail = ctx.formParam("username");
        String password = ctx.formParam("password");
        boolean rememberMe = Boolean.parseBoolean(ctx.formParam("rememberMe"));

        if (usernameOrEmail == null || password == null) {
            ctx.status(400).result("Missing credentials");
            return;
        }

        // Clean whitespace and normalize case for emails
        usernameOrEmail = usernameOrEmail.trim();
        if (usernameOrEmail.contains("@")) {
            usernameOrEmail = usernameOrEmail.toLowerCase();
        }

        String referer = ctx.header("Referer");
        if (referer != null && !referer.contains("/admin")) {
            // Student portal strictly requires numeric ID
            if (!usernameOrEmail.matches("\\d+")) {
                ctx.status(400).result("Credential ID must be numeric.");
                return;
            }
        }

        Optional<User> authenticatedUser = identityUseCase.authenticate(usernameOrEmail, password);

        if (authenticatedUser.isPresent()) {
            User user = authenticatedUser.get();
            String token = JwtProvider.createToken(user.getId(), user.getRole().name(), rememberMe);
            
            Cookie jwtCookie = new Cookie("auth_token", token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(false);
            jwtCookie.setSameSite(SameSite.STRICT);
            jwtCookie.setPath("/");
            
            if (rememberMe) {
                jwtCookie.setMaxAge(30 * 24 * 60 * 60); 
            }
            
            ctx.cookie(jwtCookie);
            
            Cookie prefCookie = new Cookie("portal_pref", user.getRole() == Role.STUDENT ? "student" : "admin");
            prefCookie.setPath("/");
            prefCookie.setMaxAge(365 * 24 * 60 * 60);
            ctx.cookie(prefCookie);
            
            ctx.status(200).result("Login successful");
        } else {
            ctx.status(401).result("Invalid credentials, pending approval, or account suspended.");
        }
    }

    private void logout(Context ctx) {
        String token = ctx.cookie("auth_token");
        if (token != null) {
            try {
                Claims claims = JwtProvider.validateToken(token);
                blacklistRepository.blacklist(claims.getId(), claims.getExpiration().getTime());
            } catch (Exception ignored) {}
        }
        ctx.removeCookie("auth_token");
        ctx.status(200).result("Logged out and session revoked.");
    }

    private void session(Context ctx) {
        String token = ctx.cookie("auth_token");
        if (token == null) {
            ctx.status(401).json("Not authenticated");
            return;
        }
        try {
            Claims claims = JwtProvider.validateToken(token);
            Optional<User> userOpt = identityUseCase.findById(claims.getSubject());
            if (userOpt.isPresent()) {
                User u = userOpt.get();
                if (!u.isActive()) {
                    ctx.removeCookie("auth_token");
                    ctx.status(403).json("Account is suspended.");
                    return;
                }
                ctx.json(Map.of(
                    "id", u.getId(),
                    "username", u.getUsername(),
                    "role", u.getRole().name()
                ));
            }
        } catch (Exception e) {
            ctx.removeCookie("auth_token");
            ctx.status(401).json("Session invalid");
        }
    }

    private void claimAccount(Context ctx) {
        String studentId = SanitizerUtil.clean(ctx.formParam("studentId"));
        String birthdate = SanitizerUtil.clean(ctx.formParam("birthdate"));

        if (studentId == null || birthdate == null) {
            ctx.status(400).result("Missing verification details.");
            return;
        }

        try {
            String tempPassword = identityUseCase.claimAccount(studentId, birthdate);
            ctx.status(200).result(tempPassword); 
        } catch (IllegalArgumentException e) {
            ctx.status(400).result(e.getMessage());
        } catch (Exception e) {
            ctx.status(500).result("Internal error.");
        }
    }

    private void claimStaffAccount(Context ctx) {
        String employeeId = ctx.formParam("employeeId");
        String birthdate = ctx.formParam("birthdate");
        String email = ctx.formParam("email");

        if (employeeId == null || email == null || birthdate == null) {
            ctx.status(400).result("Missing staff verification details.");
            return;
        }

        // Standard trim for ID and Birthdate
        employeeId = employeeId.trim();
        birthdate = birthdate.trim();

        // GUARD CLAUSE: Enforce Institutional Domain Boundary
        try {
            String requiredDomain = identityUseCase.getInstitutionDomain();
            if (requiredDomain == null || requiredDomain.isBlank()) {
                ctx.status(400).result("Institution domain is not configured. Contact System Administrator.");
                return;
            }
            
            // AGGRESSIVE SANITIZATION: Kill all unicode spaces, \r, \n, and non-printable artifacts
            requiredDomain = requiredDomain.replaceAll("[^a-zA-Z0-9.-]", "").toLowerCase();
            String cleanEmail = email.replaceAll("[^a-zA-Z0-9.@_-]", "").toLowerCase();
            
            if (!cleanEmail.endsWith("@" + requiredDomain)) {
                // If it fails, this will expose exactly what the backend evaluated
                ctx.status(400).result(String.format("Security violation: [%s] does not match domain [@%s]", cleanEmail, requiredDomain));
                return;
            }
            
            // Safe to proceed with strictly cleaned email
            email = cleanEmail;
            
        } catch (Exception e) {
            ctx.status(500).result("Failed to verify institutional domain.");
            return;
        }

        try {
            String tempPassword = identityUseCase.claimStaffAccount(employeeId, birthdate, email);
            ctx.status(200).result(tempPassword);
        } catch (IllegalArgumentException e) {
            ctx.status(400).result(e.getMessage());
        } catch (Exception e) {
            ctx.status(500).result("Internal error.");
        }
    }

    public void changePassword(Context ctx) {
        String userId = ctx.attribute("userId"); 
        String newPassword = ctx.formParam("newPassword");

        if (userId == null) {
            ctx.status(401).result("Unauthorized.");
            return;
        }

        if (newPassword == null || newPassword.isBlank()) {
            ctx.status(400).result("Password cannot be empty.");
            return;
        }

        try {
            identityUseCase.changePassword(userId, newPassword);
            ctx.status(200).result("Password updated successfully.");
        } catch (Exception e) {
            ctx.status(500).result("Update failed.");
        }
    }

    private void provisionUser(Context ctx) {
        String creatorId = ctx.attribute("userId");
        if (creatorId == null) {
            ctx.status(401).result("Unauthorized.");
            return;
        }

        String newUsername = SanitizerUtil.clean(ctx.formParam("username"));
        String roleStr = SanitizerUtil.clean(ctx.formParam("role"));
        String birthdate = SanitizerUtil.clean(ctx.formParam("birthdate"));
        
        // NEW PARAMETERS EXTRACTED HERE
        String firstName = SanitizerUtil.clean(ctx.formParam("firstName"));
        String lastName = SanitizerUtil.clean(ctx.formParam("lastName"));
        String middleInitial = SanitizerUtil.clean(ctx.formParam("middleInitial"));

        if (newUsername == null || newUsername.isBlank() || roleStr == null || roleStr.isBlank()) {
            ctx.status(400).result("Missing username or role.");
            return;
        }

        try {
            Role targetRole = Role.valueOf(roleStr.toUpperCase());
            
            if ((birthdate == null || birthdate.isBlank()) && targetRole != Role.STUDENT) {
                birthdate = "1990-01-01"; 
            } else if (birthdate == null || birthdate.isBlank()) {
                ctx.status(400).result("Birthdate is required for provisioning Student accounts.");
                return;
            }

            Map<String, Object> result = identityUseCase.provisionUser(
                creatorId, newUsername, birthdate, targetRole, 
                firstName == null ? "" : firstName, 
                lastName == null ? "" : lastName, 
                middleInitial == null ? "" : middleInitial
            );
            
            User provisionedUser = (User) result.get("user");
            
            ctx.status(201).json(Map.of(
                "message", "User provisioned successfully",
                "userId", provisionedUser.getId(),
                "username", provisionedUser.getUsername(),
                "role", provisionedUser.getRole().name()
            ));

        } catch (IllegalArgumentException | SecurityException e) {
            ctx.status(400).result(e.getMessage());
        } catch (Exception e) {
            ctx.status(500).result("Internal provisioning error.");
        }
    }

    private void approveRegistrar(Context ctx) {
        String directorId = ctx.attribute("userId");
        String targetId = SanitizerUtil.clean(ctx.formParam("targetId"));
        
        if (directorId == null) {
            ctx.status(401).result("Unauthorized.");
            return;
        }
        if (targetId == null || targetId.isBlank()) {
            ctx.status(400).result("Missing target ID.");
            return;
        }
        
        try {
            identityUseCase.approveRegistrar(directorId, targetId);
            ctx.status(200).result("Registrar approved successfully.");
        } catch (SecurityException | IllegalArgumentException e) {
            ctx.status(400).result(e.getMessage());
        } catch (Exception e) {
            ctx.status(500).result("Approval failed.");
        }
    }

    private void getDomain(Context ctx) {
        try {
            String domain = identityUseCase.getInstitutionDomain();
            ctx.status(200).json(Map.of("domain", domain == null ? "" : domain));
        } catch (Exception e) {
            ctx.status(500).result("Failed to fetch domain.");
        }
    }

    private void setDomain(Context ctx) {
        String adminId = ctx.attribute("userId");
        String password = ctx.formParam("password");
        String domain = SanitizerUtil.clean(ctx.formParam("domain"));

        if (adminId == null || password == null || domain == null) {
            ctx.status(400).result("Missing required parameters.");
            return;
        }

        try {
            identityUseCase.updateInstitutionDomain(adminId, password, domain);
            ctx.status(200).result("Domain locked successfully.");
        } catch (SecurityException | IllegalArgumentException e) {
            ctx.status(400).result(e.getMessage());
        } catch (Exception e) {
            ctx.status(500).result("Internal error.");
        }
    }

    private void getAllUsers(Context ctx) {
        String requestingUserId = ctx.attribute("userId");
        try {
            List<Map<String, Object>> safeList = identityUseCase.getAllUsers(requestingUserId).stream()
                .map(u -> Map.<String, Object>of(
                    "id", u.getId(),
                    "username", u.getUsername(),
                    "fullName", u.getFullName(), // Added for dashboard roster
                    "email", u.getEmail() == null ? "N/A" : u.getEmail(),
                    "role", u.getRole().name(),
                    "status", u.isActive() ? 
                        (u.isClaimed() ? "Active" : "Unclaimed") : "Suspended",
                    "isApproved", u.isApproved() // Expose approval state
                )).toList();
            ctx.status(200).json(safeList);
        } catch (Exception e) {
            ctx.status(403).result(e.getMessage());
        }
    }

    private void clearCache(Context ctx) {
        ctx.removeCookie("portal_pref");
        ctx.removeCookie("auth_token");
        ctx.status(200).result("Cookies cleared. Client must clear localStorage.");
    }
}